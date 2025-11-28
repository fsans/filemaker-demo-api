package com.filemaker.demo.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filemaker.demo.dto.ContactDTO;
import com.filemaker.demo.entity.Contact;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public class ContactIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void testCompleteContactWorkflow() throws Exception {
        // 1. Create a new contact
        ContactDTO newContact = new ContactDTO();
        newContact.setEmail("workflow@example.com");
        newContact.setLogin("workflowuser");
        newContact.setPassword("workflowpass");
        newContact.setFirstName("Workflow");
        newContact.setLastName("Test");
        newContact.setCompany("Workflow Company");
        newContact.setJobTitle("Integration Tester");
        newContact.setWebsite("https://workflow.com");
        newContact.setNotes("Created for integration testing");

        String createResponse = mockMvc.perform(post("/api/contacts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newContact)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.email", equalTo(newContact.getEmail())))
                .andExpect(jsonPath("$.firstName", equalTo(newContact.getFirstName())))
                .andExpect(jsonPath("$.lastName", equalTo(newContact.getLastName())))
                .andExpect(jsonPath("$.company", equalTo(newContact.getCompany())))
                .andReturn().getResponse().getContentAsString();

        // Extract the created contact ID
        Contact createdContact = objectMapper.readValue(createResponse, Contact.class);
        Long contactId = createdContact.getId();

        // 2. Get the contact by ID
        mockMvc.perform(get("/api/contacts/{id}", contactId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(contactId.intValue())))
                .andExpect(jsonPath("$.email", equalTo(newContact.getEmail())));

        // 3. Update the contact
        ContactDTO updateContact = new ContactDTO();
        updateContact.setEmail("updated.workflow@example.com");
        updateContact.setLogin("workflowuser");
        updateContact.setPassword("workflowpass");
        updateContact.setFirstName("Workflow");
        updateContact.setLastName("Updated");
        updateContact.setCompany("Updated Company");
        updateContact.setJobTitle("Senior Integration Tester");
        updateContact.setWebsite("https://updated-workflow.com");
        updateContact.setNotes("Updated for integration testing");

        mockMvc.perform(put("/api/contacts/{id}", contactId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateContact)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(contactId.intValue())))
                .andExpect(jsonPath("$.email", equalTo(updateContact.getEmail())))
                .andExpect(jsonPath("$.lastName", equalTo(updateContact.getLastName())))
                .andExpect(jsonPath("$.company", equalTo(updateContact.getCompany())))
                .andExpect(jsonPath("$.jobTitle", equalTo(updateContact.getJobTitle())));

        // 4. Partial update the contact
        ContactDTO patchContact = new ContactDTO();
        patchContact.setNotes("Partially updated notes");
        patchContact.setWebsite("https://partially-updated.com");

        mockMvc.perform(patch("/api/contacts/{id}", contactId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchContact)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(contactId.intValue())))
                .andExpect(jsonPath("$.notes", equalTo(patchContact.getNotes())))
                .andExpect(jsonPath("$.website", equalTo(patchContact.getWebsite())))
                // Verify other fields remain unchanged
                .andExpect(jsonPath("$.email", equalTo(updateContact.getEmail())))
                .andExpect(jsonPath("$.jobTitle", equalTo(updateContact.getJobTitle())));

        // 5. Search for the contact
        mockMvc.perform(get("/api/contacts/search")
                .param("q", "Workflow")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.content[?(@.id == " + contactId + ")]").exists());

        // 6. Get contacts by company
        mockMvc.perform(get("/api/contacts/by-company/{company}", updateContact.getCompany()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[?(@.id == " + contactId + ")]").exists());

        // 7. Test photo upload (if container field is available)
        MockMultipartFile photoFile = new MockMultipartFile(
                "file",
                "test-photo.jpg",
                "image/jpeg",
                "test photo content".getBytes()
        );

        mockMvc.perform(multipart("/api/contacts/{id}/photo", contactId)
                .file(photoFile))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Photo uploaded successfully")));

        // 8. Get photo info
        mockMvc.perform(get("/api/contacts/{id}/photo/info", contactId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contactId", equalTo(contactId.intValue())))
                .andExpect(jsonPath("$.hasPhoto", is(true)));

        // 9. Download photo
        mockMvc.perform(get("/api/contacts/{id}/photo", contactId))
                .andExpect(status().isOk());

        // 10. Delete photo
        mockMvc.perform(delete("/api/contacts/{id}/photo", contactId))
                .andExpect(status().isNoContent());

        // 11. Verify photo is deleted
        mockMvc.perform(get("/api/contacts/{id}/photo/info", contactId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPhoto", is(false)));

        // 12. Get all contacts and verify our contact is in the list
        mockMvc.perform(get("/api/contacts")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.content[?(@.id == " + contactId + ")]").exists());

        // 13. Test pagination
        mockMvc.perform(get("/api/contacts")
                .param("page", "0")
                .param("size", "5")
                .param("sortBy", "email")
                .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(5))))
                .andExpect(jsonPath("$.number", equalTo(0)))
                .andExpect(jsonPath("$.size", equalTo(5)))
                .andExpect(jsonPath("$.sort[0].property", equalTo("email")))
                .andExpect(jsonPath("$.sort[0].direction", equalTo("ASC")));

        // 14. Create additional contacts for testing
        for (int i = 0; i < 10; i++) {
            ContactDTO additionalContact = new ContactDTO();
            additionalContact.setEmail("additional" + i + "@example.com");
            additionalContact.setLogin("additional" + i);
            additionalContact.setPassword("pass" + i);
            additionalContact.setFirstName("Additional" + i);
            additionalContact.setLastName("User");
            additionalContact.setCompany("Additional Company");

            mockMvc.perform(post("/api/contacts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(additionalContact)))
                    .andExpect(status().isCreated());
        }

        // 15. Test pagination with more data
        mockMvc.perform(get("/api/contacts")
                .param("page", "0")
                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.totalElements", greaterThan(10)))
                .andExpect(jsonPath("$.totalPages", greaterThan(2)));

        // 16. Test search with pagination
        mockMvc.perform(get("/api/contacts/search")
                .param("q", "Additional")
                .param("page", "0")
                .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements", equalTo(10)));

        // 17. Clean up - delete the test contact
        mockMvc.perform(delete("/api/contacts/{id}", contactId))
                .andExpect(status().isNoContent());

        // 18. Verify the contact is deleted
        mockMvc.perform(get("/api/contacts/{id}", contactId))
                .andExpect(status().isNotFound());
    }

    @Test
    void testErrorHandlingWorkflow() throws Exception {
        // Test getting non-existent contact
        mockMvc.perform(get("/api/contacts/{id}", 99999L))
                .andExpect(status().isNotFound());

        // Test updating non-existent contact
        ContactDTO updateContact = new ContactDTO();
        updateContact.setEmail("test@example.com");
        updateContact.setLogin("test");
        updateContact.setPassword("test");

        mockMvc.perform(put("/api/contacts/{id}", 99999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateContact)))
                .andExpect(status().isNotFound());

        // Test patching non-existent contact
        mockMvc.perform(patch("/api/contacts/{id}", 99999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateContact)))
                .andExpect(status().isNotFound());

        // Test deleting non-existent contact
        mockMvc.perform(delete("/api/contacts/{id}", 99999L))
                .andExpect(status().isNotFound());

        // Test photo operations on non-existent contact
        MockMultipartFile photoFile = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test".getBytes()
        );

        mockMvc.perform(multipart("/api/contacts/{id}/photo", 99999L)
                .file(photoFile))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/contacts/{id}/photo", 99999L))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/contacts/{id}/photo", 99999L))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/contacts/{id}/photo/info", 99999L))
                .andExpect(status().isNotFound());

        // Test creating contact with invalid data
        ContactDTO invalidContact = new ContactDTO();
        // Missing required fields

        mockMvc.perform(post("/api/contacts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidContact)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSearchAndFilterWorkflow() throws Exception {
        // Create contacts with different attributes
        String[] companies = {"TechCorp", "DataInc", "CloudSys"};
        String[] jobTitles = {"Developer", "Manager", "Analyst"};
        String[] firstNames = {"Alice", "Bob", "Charlie"};

        for (int i = 0; i < companies.length; i++) {
            for (int j = 0; j < jobTitles.length; j++) {
                ContactDTO contact = new ContactDTO();
                contact.setEmail(firstNames[i] + jobTitles[j] + "@example.com");
                contact.setLogin(firstNames[i].toLowerCase() + jobTitles[j].toLowerCase());
                contact.setPassword("password");
                contact.setFirstName(firstNames[i]);
                contact.setLastName("TestUser");
                contact.setCompany(companies[i]);
                contact.setJobTitle(jobTitles[j]);
                contact.setNotes("Works at " + companies[i] + " as " + jobTitles[j]);

                mockMvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contact)))
                        .andExpect(status().isCreated());
            }
        }

        // Search by first name
        mockMvc.perform(get("/api/contacts/search")
                .param("q", "Alice")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[*].firstName", everyItem(equalTo("Alice"))));

        // Search by company
        mockMvc.perform(get("/api/contacts/search")
                .param("q", "TechCorp")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[*].company", everyItem(equalTo("TechCorp"))));

        // Search by job title
        mockMvc.perform(get("/api/contacts/search")
                .param("q", "Developer")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[*].jobTitle", everyItem(equalTo("Developer"))));

        // Get contacts by specific company
        mockMvc.perform(get("/api/contacts/by-company/{company}", "DataInc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].company", everyItem(equalTo("DataInc"))));

        // Search with no results
        mockMvc.perform(get("/api/contacts/search")
                .param("q", "NonExistent")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", equalTo(0)));

        // Get contacts from non-existent company
        mockMvc.perform(get("/api/contacts/by-company/{company}", "NonExistentCompany"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
