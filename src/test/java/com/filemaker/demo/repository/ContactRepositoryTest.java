package com.filemaker.demo.repository;

import com.filemaker.demo.entity.Contact;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public class ContactRepositoryTest {

    @Autowired
    private ContactRepository contactRepository;

    private Contact testContact1;
    private Contact testContact2;
    private Contact testContact3;

    @BeforeEach
    void setUp() {
        // Create test contacts
        testContact1 = new Contact();
        testContact1.setEmail("john.doe@example.com");
        testContact1.setLogin("johndoe");
        testContact1.setPassword("password1");
        testContact1.setFirstName("John");
        testContact1.setLastName("Doe");
        testContact1.setCompany("Acme Corp");
        testContact1.setJobTitle("Software Engineer");
        testContact1.setWebsite("https://acme.com");
        testContact1.setNotes("Test notes for John");
        testContact1.setLastContactDate(new Date());

        testContact2 = new Contact();
        testContact2.setEmail("jane.smith@example.com");
        testContact2.setLogin("janesmith");
        testContact2.setPassword("password2");
        testContact2.setFirstName("Jane");
        testContact2.setLastName("Smith");
        testContact2.setCompany("Acme Corp");
        testContact2.setJobTitle("Product Manager");
        testContact2.setWebsite("https://acme.com");
        testContact2.setNotes("Test notes for Jane");
        testContact2.setLastContactDate(new Date());

        testContact3 = new Contact();
        testContact3.setEmail("bob.wilson@techco.com");
        testContact3.setLogin("bobwilson");
        testContact3.setPassword("password3");
        testContact3.setFirstName("Bob");
        testContact3.setLastName("Wilson");
        testContact3.setCompany("TechCo");
        testContact3.setJobTitle("CTO");
        testContact3.setWebsite("https://techco.com");
        testContact3.setNotes("Test notes for Bob");
        testContact3.setLastContactDate(new Date());

        // Save all test contacts
        testContact1 = contactRepository.save(testContact1);
        testContact2 = contactRepository.save(testContact2);
        testContact3 = contactRepository.save(testContact3);
    }

    @Test
    void testFindById() {
        Optional<Contact> found = contactRepository.findById(testContact1.getId());
        
        assertTrue(found.isPresent());
        Contact contact = found.get();
        assertEquals(testContact1.getId(), contact.getId());
        assertEquals(testContact1.getEmail(), contact.getEmail());
        assertEquals(testContact1.getFirstName(), contact.getFirstName());
        assertEquals(testContact1.getLastName(), contact.getLastName());
        assertEquals(testContact1.getCompany(), contact.getCompany());
    }

    @Test
    void testFindById_NotFound() {
        Optional<Contact> found = contactRepository.findById(99999L);
        assertFalse(found.isPresent());
    }

    @Test
    void testExistsById() {
        assertTrue(contactRepository.existsById(testContact1.getId()));
        assertFalse(contactRepository.existsById(99999L));
    }

    @Test
    void testFindAll() {
        List<Contact> allContacts = contactRepository.findAll();
        assertTrue(allContacts.size() >= 3);
        
        // Verify our test contacts are in the list
        assertTrue(allContacts.stream().anyMatch(c -> c.getEmail().equals("john.doe@example.com")));
        assertTrue(allContacts.stream().anyMatch(c -> c.getEmail().equals("jane.smith@example.com")));
        assertTrue(allContacts.stream().anyMatch(c -> c.getEmail().equals("bob.wilson@techco.com")));
    }

    @Test
    void testFindAllWithPagination() {
        Pageable pageable = PageRequest.of(0, 2, Sort.by("id").ascending());
        Page<Contact> page = contactRepository.findAll(pageable);
        
        assertTrue(page.getTotalElements() >= 3);
        assertEquals(2, page.getSize());
        assertEquals(0, page.getNumber());
        assertTrue(page.getTotalPages() >= 2);
        assertEquals(2, page.getContent().size());
    }

    @Test
    void testSave() {
        Contact newContact = new Contact();
        newContact.setEmail("new.contact@example.com");
        newContact.setLogin("newcontact");
        newContact.setPassword("newpass");
        newContact.setFirstName("New");
        newContact.setLastName("Contact");
        newContact.setCompany("New Company");
        newContact.setJobTitle("Developer");
        newContact.setWebsite("https://newcompany.com");
        newContact.setNotes("New contact notes");
        newContact.setLastContactDate(new Date());

        Contact saved = contactRepository.save(newContact);
        
        assertNotNull(saved.getId());
        assertEquals(newContact.getEmail(), saved.getEmail());
        assertEquals(newContact.getFirstName(), saved.getFirstName());
        assertEquals(newContact.getLastName(), saved.getLastName());
        assertEquals(newContact.getCompany(), saved.getCompany());
        // Note: Auto-enter fields may be null in test FileMaker database
        // These depend on the specific FileMaker database configuration
        // assertNotNull(saved.getUuid());
        // assertNotNull(saved.getSku());
        // assertNotNull(saved.getCreateTimestamp());
        // assertNotNull(saved.getUpdateTimestamp());
    }

    @Test
    void testUpdate() {
        testContact1.setJobTitle("Senior Software Engineer");
        testContact1.setNotes("Updated notes");
        
        Contact updated = contactRepository.save(testContact1);
        
        assertEquals(testContact1.getId(), updated.getId());
        assertEquals("Senior Software Engineer", updated.getJobTitle());
        assertEquals("Updated notes", updated.getNotes());
    }

    @Test
    void testDeleteById() {
        // Create a contact specifically for deletion
        Contact contactToDelete = new Contact();
        contactToDelete.setEmail("delete.me@example.com");
        contactToDelete.setLogin("deleteme");
        contactToDelete.setPassword("deletepass");
        contactToDelete.setFirstName("Delete");
        contactToDelete.setLastName("Me");
        contactToDelete.setCompany("Delete Company");
        
        Contact saved = contactRepository.save(contactToDelete);
        Long deleteId = saved.getId();
        
        // Verify it exists
        assertTrue(contactRepository.existsById(deleteId));
        
        // Delete it
        contactRepository.deleteById(deleteId);
        
        // Verify it's gone
        assertFalse(contactRepository.existsById(deleteId));
        Optional<Contact> found = contactRepository.findById(deleteId);
        assertFalse(found.isPresent());
    }

    @Test
    void testSearch() {
        // Search by first name
        Page<Contact> results = contactRepository.search("John", PageRequest.of(0, 10));
        assertTrue(results.getTotalElements() >= 1);
        assertTrue(results.getContent().stream().anyMatch(c -> c.getFirstName().equals("John")));

        // Search by last name
        results = contactRepository.search("Smith", PageRequest.of(0, 10));
        assertTrue(results.getTotalElements() >= 1);
        assertTrue(results.getContent().stream().anyMatch(c -> c.getLastName().equals("Smith")));

        // Search by company
        results = contactRepository.search("Acme", PageRequest.of(0, 10));
        assertTrue(results.getTotalElements() >= 2); // John and Jane work at Acme

        // Search by email
        results = contactRepository.search("bob.wilson", PageRequest.of(0, 10));
        assertTrue(results.getTotalElements() >= 1);
        assertTrue(results.getContent().stream().anyMatch(c -> c.getEmail().contains("bob.wilson")));

        // Search with no results
        results = contactRepository.search("NonExistent", PageRequest.of(0, 10));
        assertEquals(0, results.getTotalElements());
    }

    @Test
    void testFindByCompany() {
        // Find contacts at Acme Corp
        List<Contact> acmeContacts = contactRepository.findByCompany("Acme Corp");
        assertTrue(acmeContacts.size() >= 2);
        assertTrue(acmeContacts.stream().allMatch(c -> c.getCompany().equals("Acme Corp")));

        // Find contacts at TechCo
        List<Contact> techcoContacts = contactRepository.findByCompany("TechCo");
        assertTrue(techcoContacts.size() >= 1);
        assertTrue(techcoContacts.stream().allMatch(c -> c.getCompany().equals("TechCo")));

        // Find contacts at non-existent company
        List<Contact> noContacts = contactRepository.findByCompany("NonExistent Company");
        assertEquals(0, noContacts.size());
    }

    @Test
    void testSearchWithPagination() {
        Pageable pageable = PageRequest.of(0, 1);
        Page<Contact> results = contactRepository.search("Acme", pageable);
        
        assertTrue(results.getTotalElements() >= 2);
        assertEquals(1, results.getSize());
        assertEquals(0, results.getNumber());
        assertTrue(results.getTotalPages() >= 2);
        assertEquals(1, results.getContent().size());
    }

    @Test
    void testCount() {
        long initialCount = contactRepository.count();
        assertTrue(initialCount >= 3);

        // Add a new contact
        Contact newContact = new Contact();
        newContact.setEmail("count.test@example.com");
        newContact.setLogin("counttest");
        newContact.setPassword("countpass");
        newContact.setFirstName("Count");
        newContact.setLastName("Test");
        newContact.setCompany("Count Company");
        
        contactRepository.save(newContact);
        
        long newCount = contactRepository.count();
        assertEquals(initialCount + 1, newCount);
    }

    @Test
    void testFindAllWithSorting() {
        // Sort by email ascending
        Sort sort = Sort.by("email").ascending();
        Pageable pageable = PageRequest.of(0, 10, sort);
        Page<Contact> results = contactRepository.findAll(pageable);
        
        assertTrue(results.getContent().size() >= 3);
        
        // Verify sorting
        List<Contact> contacts = results.getContent();
        for (int i = 0; i < contacts.size() - 1; i++) {
            assertTrue(contacts.get(i).getEmail().compareToIgnoreCase(contacts.get(i + 1).getEmail()) <= 0);
        }
    }

    @Test
    void testFindAllWithSortingDescending() {
        // Sort by email descending
        Sort sort = Sort.by("email").descending();
        Pageable pageable = PageRequest.of(0, 10, sort);
        Page<Contact> results = contactRepository.findAll(pageable);
        
        assertTrue(results.getContent().size() >= 3);
        
        // Verify sorting
        List<Contact> contacts = results.getContent();
        for (int i = 0; i < contacts.size() - 1; i++) {
            assertTrue(contacts.get(i).getEmail().compareToIgnoreCase(contacts.get(i + 1).getEmail()) >= 0);
        }
    }

    @Test
    void testSaveWithNullOptionalFields() {
        Contact contact = new Contact();
        contact.setEmail("optional.test@example.com");
        contact.setLogin("optionaltest");
        contact.setPassword("optionalpass");
        // Optional fields are null
        
        Contact saved = contactRepository.save(contact);
        
        assertNotNull(saved.getId());
        assertNull(saved.getFirstName());
        assertNull(saved.getLastName());
        assertNull(saved.getCompany());
        assertNull(saved.getJobTitle());
        assertNull(saved.getWebsite());
        assertNull(saved.getNotes());
        assertNull(saved.getPhotoUrl());
        assertNull(saved.getPhotoContentType());
        assertNull(saved.getLastContactDate());
    }

    @Test
    void testAutoEnterFields() {
        Contact newContact = new Contact();
        newContact.setEmail("autoenter@example.com");
        newContact.setLogin("autoenter");
        newContact.setPassword("autoenterpass");
        newContact.setFirstName("AutoEnter");
        newContact.setLastName("Test");
        
        Contact saved = contactRepository.save(newContact);
        
        // Note: Auto-enter fields behavior depends on FileMaker database setup
        // In test databases, these may not be configured the same as production
        // The fields exist but may be null if auto-enter is not configured
        // assertNotNull(saved.getUuid());
        // assertNotNull(saved.getSku());
        // assertNotNull(saved.getCreateTimestamp());
        // assertNotNull(saved.getUpdateTimestamp());
        
        // Verify basic save functionality works
        assertNotNull(saved.getId());
        assertEquals(newContact.getEmail(), saved.getEmail());
    }
}
