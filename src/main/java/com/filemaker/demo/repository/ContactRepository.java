package com.filemaker.demo.repository;

import com.filemaker.demo.entity.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long>, ContactRepositoryCustom {

    // Find by email
    List<Contact> findByEmail(String email);

    // Find by company
    List<Contact> findByCompany(String company);

    // Search by name (first or last)
    @Query("SELECT c FROM Contact c WHERE c.firstName LIKE %:name% OR c.lastName LIKE %:name%")
    List<Contact> searchByName(@Param("name") String name);

    // Find by company with pagination
    Page<Contact> findByCompany(String company, Pageable pageable);

    // Search contacts
    @Query("SELECT c FROM Contact c WHERE " +
           "c.firstName LIKE %:query% OR " +
           "c.lastName LIKE %:query% OR " +
           "c.email LIKE %:query% OR " +
           "c.company LIKE %:query%")
    Page<Contact> search(@Param("query") String query, Pageable pageable);

    // Native query with embedded pagination for FileMaker
    // FileMaker doesn't support parameterized OFFSET/FETCH
    @Query(value = "SELECT * FROM contact ORDER BY id OFFSET :offset ROWS FETCH FIRST :limit ROWS ONLY", 
           nativeQuery = true)
    List<Contact> findAllPaginated(@Param("offset") int offset, @Param("limit") int limit);

    // Count for pagination
    @Query(value = "SELECT COUNT(*) FROM contact", nativeQuery = true)
    long countAll();
}
