package com.filemaker.demo.repository;

import com.filemaker.demo.entity.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Custom repository interface for FileMaker-specific pagination.
 * FileMaker doesn't support parameterized OFFSET/FETCH clauses.
 */
public interface ContactRepositoryCustom {
    
    /**
     * Find all contacts with FileMaker-compatible pagination.
     * Uses native SQL with embedded OFFSET/FETCH values.
     */
    Page<Contact> findAllWithPagination(Pageable pageable);
}
