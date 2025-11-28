package com.filemaker.demo.controller;

import com.filemaker.demo.entity.Contact;
import com.filemaker.demo.repository.ContactRepository;
import com.filemaker.demo.service.ContainerFieldService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.Date;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public class PhotoControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ContactRepository contactRepository;

    @MockBean
    private ContainerFieldService containerFieldService;

    private MockMvc mockMvc;
    private Contact testContact;
    private byte[] testImageData;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Create a test contact for each test method
        testContact = new Contact();
        testContact.setEmail("phototest@example.com");
        testContact.setLogin("phototest");
        testContact.setPassword("testpass");
        testContact.setFirstName("Photo");
        testContact.setLastName("Test");
        testContact.setCompany("Photo Company");
        testContact.setPhotoContentType("image/jpeg");
        testContact.setLastContactDate(new Date());
        
        testContact = contactRepository.save(testContact);
        
        // Create test image data
        testImageData = "fake image data for testing".getBytes();
    }

    @Test
    void testUploadPhoto() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", 
                "test.jpg", 
                "image/jpeg", 
                testImageData
        );

        when(containerFieldService.uploadToContainer(anyString(), anyString(), anyLong(), any()))
                .thenReturn(true);

        mockMvc.perform(multipart("/api/contacts/{id}/photo", testContact.getId())
                .file(file))
                .andExpect(status().isOk())
                .andExpect(content().string("Photo uploaded successfully"));

        verify(containerFieldService).uploadToContainer("contact", "photo_content", testContact.getId(), file);
    }

    @Test
    void testUploadPhoto_ContactNotFound() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", 
                "test.jpg", 
                "image/jpeg", 
                testImageData
        );

        mockMvc.perform(multipart("/api/contacts/{id}/photo", 99999L)
                .file(file))
                .andExpect(status().isNotFound());

        verify(containerFieldService, never()).uploadToContainer(anyString(), anyString(), anyLong(), any());
    }

    @Test
    void testUploadPhoto_EmptyFile() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", 
                "empty.jpg", 
                "image/jpeg", 
                new byte[0]
        );

        mockMvc.perform(multipart("/api/contacts/{id}/photo", testContact.getId())
                .file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("File is empty"));

        verify(containerFieldService, never()).uploadToContainer(anyString(), anyString(), anyLong(), any());
    }

    @Test
    void testUploadPhoto_UploadFailed() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", 
                "test.jpg", 
                "image/jpeg", 
                testImageData
        );

        when(containerFieldService.uploadToContainer(anyString(), anyString(), anyLong(), any()))
                .thenReturn(false);

        mockMvc.perform(multipart("/api/contacts/{id}/photo", testContact.getId())
                .file(file))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Failed to upload photo"));
    }

    @Test
    void testDownloadPhoto() throws Exception {
        when(containerFieldService.downloadFromContainer(anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(testImageData);

        mockMvc.perform(get("/api/contacts/{id}/photo", testContact.getId())
                .param("format", "JPEG"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(testImageData))
                .andExpect(header().string("Content-Length", String.valueOf(testImageData.length)))
                .andExpect(header().string("Content-Disposition", containsString("attachment")));

        verify(containerFieldService).downloadFromContainer("contact", "photo_content", testContact.getId(), "JPEG");
    }

    @Test
    void testDownloadPhoto_ContactNotFound() throws Exception {
        mockMvc.perform(get("/api/contacts/{id}/photo", 99999L))
                .andExpect(status().isNotFound());

        verify(containerFieldService, never()).downloadFromContainer(anyString(), anyString(), anyLong(), anyString());
    }

    @Test
    void testDownloadPhoto_NoPhotoData() throws Exception {
        when(containerFieldService.downloadFromContainer(anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(null);

        mockMvc.perform(get("/api/contacts/{id}/photo", testContact.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDownloadPhoto_EmptyPhotoData() throws Exception {
        when(containerFieldService.downloadFromContainer(anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(new byte[0]);

        mockMvc.perform(get("/api/contacts/{id}/photo", testContact.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDownloadPhoto_WithAutoDetection() throws Exception {
        // Test when format is not specified - should auto-detect from content type
        when(containerFieldService.downloadFromContainer(anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(testImageData);

        mockMvc.perform(get("/api/contacts/{id}/photo", testContact.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(testImageData));
    }

    @Test
    void testViewPhotoInline() throws Exception {
        when(containerFieldService.downloadFromContainer(anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(testImageData);

        mockMvc.perform(get("/api/contacts/{id}/photo/inline", testContact.getId())
                .param("format", "JPEG"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(testImageData))
                .andExpect(header().string("Content-Disposition", "inline"));

        verify(containerFieldService).downloadFromContainer("contact", "photo_content", testContact.getId(), "JPEG");
    }

    @Test
    void testViewPhotoInline_ContactNotFound() throws Exception {
        mockMvc.perform(get("/api/contacts/{id}/photo/inline", 99999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeletePhoto() throws Exception {
        when(containerFieldService.clearContainer(anyString(), anyString(), anyLong()))
                .thenReturn(true);

        mockMvc.perform(delete("/api/contacts/{id}/photo", testContact.getId()))
                .andExpect(status().isNoContent());

        verify(containerFieldService).clearContainer("contact", "photo_content", testContact.getId());
    }

    @Test
    void testDeletePhoto_ContactNotFound() throws Exception {
        mockMvc.perform(delete("/api/contacts/{id}/photo", 99999L))
                .andExpect(status().isNotFound());

        verify(containerFieldService, never()).clearContainer(anyString(), anyString(), anyLong());
    }

    @Test
    void testDeletePhoto_DeleteFailed() throws Exception {
        when(containerFieldService.clearContainer(anyString(), anyString(), anyLong()))
                .thenReturn(false);

        mockMvc.perform(delete("/api/contacts/{id}/photo", testContact.getId()))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testGetPhotoInfo() throws Exception {
        when(containerFieldService.getContainerReference(anyString(), anyString(), anyLong()))
                .thenReturn("test.jpg");

        mockMvc.perform(get("/api/contacts/{id}/photo/info", testContact.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.contactId", equalTo(testContact.getId().intValue())))
                .andExpect(jsonPath("$.contentType", equalTo(testContact.getPhotoContentType())))
                .andExpect(jsonPath("$.reference", equalTo("test.jpg")))
                .andExpect(jsonPath("$.hasPhoto", equalTo(true)));

        verify(containerFieldService).getContainerReference("contact", "photo_content", testContact.getId());
    }

    @Test
    void testGetPhotoInfo_ContactNotFound() throws Exception {
        mockMvc.perform(get("/api/contacts/{id}/photo/info", 99999L))
                .andExpect(status().isNotFound());

        verify(containerFieldService, never()).getContainerReference(anyString(), anyString(), anyLong());
    }

    @Test
    void testGetPhotoInfo_NoPhoto() throws Exception {
        when(containerFieldService.getContainerReference(anyString(), anyString(), anyLong()))
                .thenReturn(null);

        mockMvc.perform(get("/api/contacts/{id}/photo/info", testContact.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPhoto", equalTo(false)))
                .andExpect(jsonPath("$.reference", equalTo(null)));
    }

    @Test
    void testDownloadPhoto_DifferentFormats() throws Exception {
        String[] formats = {"JPEG", "PNGf", "GIFf", "TIFF", "PDF"};
        String[] contentTypes = {"image/jpeg", "image/png", "image/gif", "image/tiff", "application/pdf"};

        for (int i = 0; i < formats.length; i++) {
            when(containerFieldService.downloadFromContainer(anyString(), anyString(), anyLong(), eq(formats[i])))
                    .thenReturn(testImageData);

            mockMvc.perform(get("/api/contacts/{id}/photo", testContact.getId())
                    .param("format", formats[i]))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(contentTypes[i]))
                    .andExpect(content().bytes(testImageData));
        }
    }

    @Test
    void testUploadPhoto_DifferentFileTypes() throws Exception {
        String[] fileNames = {"test.jpg", "test.png", "test.gif", "test.pdf"};
        String[] contentTypes = {"image/jpeg", "image/png", "image/gif", "application/pdf"};

        for (int i = 0; i < fileNames.length; i++) {
            MockMultipartFile file = new MockMultipartFile(
                    "file", 
                    fileNames[i], 
                    contentTypes[i], 
                    testImageData
            );

            when(containerFieldService.uploadToContainer(anyString(), anyString(), anyLong(), any()))
                    .thenReturn(true);

            mockMvc.perform(multipart("/api/contacts/{id}/photo", testContact.getId())
                    .file(file))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Photo uploaded successfully"));
        }
    }
}
