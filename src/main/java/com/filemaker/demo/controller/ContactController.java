package com.filemaker.demo.controller;

import com.filemaker.demo.dto.ContactDTO;
import com.filemaker.demo.entity.Contact;
import com.filemaker.demo.repository.ContactRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.lang.NonNull;

import java.util.List;

@RestController
@RequestMapping("/api/contacts")
@Tag(name = "Contacts", description = "CRUD operations for FileMaker Contacts")
public class ContactController {

    private final ContactRepository contactRepository;

    public ContactController(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    // ==================== READ ====================

    @GetMapping
    @Operation(summary = "Get all contacts", description = "Returns all contacts with pagination support")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved contacts")
    })
    public ResponseEntity<Page<Contact>> getAllContacts(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "asc") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("desc") 
            ? Sort.by(sortBy).descending() 
            : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        // Use standard Spring Data pagination - dialect handles FileMaker-specific SQL
        return ResponseEntity.ok(contactRepository.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get contact by ID", description = "Returns a single contact by its ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Contact found"),
        @ApiResponse(responseCode = "404", description = "Contact not found")
    })
    public ResponseEntity<Contact> getContactById(
            @Parameter(description = "Contact ID") @PathVariable @NonNull Long id
    ) {
        return contactRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    @Operation(summary = "Search contacts", description = "Search contacts by name, email, or company")
    public ResponseEntity<Page<Contact>> searchContacts(
            @Parameter(description = "Search query") @RequestParam String q,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(contactRepository.search(q, pageable));
    }

    @GetMapping("/by-company/{company}")
    @Operation(summary = "Get contacts by company", description = "Returns all contacts for a specific company")
    public ResponseEntity<List<Contact>> getContactsByCompany(
            @Parameter(description = "Company name") @PathVariable String company
    ) {
        return ResponseEntity.ok(contactRepository.findByCompany(company));
    }

    // ==================== CREATE ====================

    @PostMapping
    @Operation(summary = "Create contact", description = "Creates a new contact")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Contact created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ResponseEntity<Contact> createContact(@RequestBody @NonNull ContactDTO dto) {
        Contact contact = new Contact();
        mapDtoToEntity(dto, contact);
        Contact saved = contactRepository.save(contact);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ==================== UPDATE ====================

    @PutMapping("/{id}")
    @Operation(summary = "Update contact", description = "Updates an existing contact")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Contact updated successfully"),
        @ApiResponse(responseCode = "404", description = "Contact not found")
    })
    @SuppressWarnings("null")
    public ResponseEntity<Contact> updateContact(
            @Parameter(description = "Contact ID") @PathVariable @NonNull Long id,
            @RequestBody @NonNull ContactDTO dto
    ) {
        return contactRepository.findById(id)
                .map(existing -> {
                    mapDtoToEntity(dto, existing);
                    return ResponseEntity.ok(contactRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partial update contact", description = "Updates specific fields of a contact")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Contact updated successfully"),
        @ApiResponse(responseCode = "404", description = "Contact not found")
    })
    @SuppressWarnings("null")
    public ResponseEntity<Contact> patchContact(
            @Parameter(description = "Contact ID") @PathVariable @NonNull Long id,
            @RequestBody @NonNull ContactDTO dto
    ) {
        return contactRepository.findById(id)
                .map(existing -> {
                    // Only update non-null fields
                    if (dto.getEmail() != null) existing.setEmail(dto.getEmail());
                    if (dto.getLogin() != null) existing.setLogin(dto.getLogin());
                    if (dto.getPassword() != null) existing.setPassword(dto.getPassword());
                    if (dto.getFirstName() != null) existing.setFirstName(dto.getFirstName());
                    if (dto.getLastName() != null) existing.setLastName(dto.getLastName());
                    if (dto.getTitle() != null) existing.setTitle(dto.getTitle());
                    if (dto.getJobTitle() != null) existing.setJobTitle(dto.getJobTitle());
                    if (dto.getCompany() != null) existing.setCompany(dto.getCompany());
                    if (dto.getWebsite() != null) existing.setWebsite(dto.getWebsite());
                    if (dto.getNotes() != null) existing.setNotes(dto.getNotes());
                    return ResponseEntity.ok(contactRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== DELETE ====================

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete contact", description = "Deletes a contact by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Contact deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Contact not found")
    })
    public ResponseEntity<Void> deleteContact(
            @Parameter(description = "Contact ID") @PathVariable @NonNull Long id
    ) {
        if (contactRepository.existsById(id)) {
            contactRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // ==================== HELPER ====================

    private void mapDtoToEntity(@NonNull ContactDTO dto, @NonNull Contact entity) {
        entity.setEmail(dto.getEmail());
        entity.setLogin(dto.getLogin());
        entity.setPassword(dto.getPassword());
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setTitle(dto.getTitle());
        entity.setJobTitle(dto.getJobTitle());
        entity.setCompany(dto.getCompany());
        entity.setWebsite(dto.getWebsite());
        entity.setNotes(dto.getNotes());
    }
}
