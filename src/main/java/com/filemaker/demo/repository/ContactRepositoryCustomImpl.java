package com.filemaker.demo.repository;

import com.filemaker.demo.entity.Contact;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Custom repository implementation for FileMaker-specific pagination.
 * FileMaker doesn't support parameterized OFFSET/FETCH clauses,
 * so we build the SQL with embedded values.
 */
@Repository
public class ContactRepositoryCustomImpl implements ContactRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public Page<Contact> findAllWithPagination(Pageable pageable) {
        // Build SQL with embedded pagination values (FileMaker requirement)
        int offset = (int) pageable.getOffset();
        int limit = pageable.getPageSize();
        
        // FileMaker requires literal values in OFFSET/FETCH, not parameters
        String sql = "SELECT * FROM contact ORDER BY id " +
                     "OFFSET " + offset + " ROWS " +
                     "FETCH FIRST " + limit + " ROWS ONLY";
        
        Query query = entityManager.createNativeQuery(sql, Contact.class);
        List<Contact> content = query.getResultList();
        
        // Get total count
        String countSql = "SELECT COUNT(*) FROM contact";
        Query countQuery = entityManager.createNativeQuery(countSql);
        Object result = countQuery.getSingleResult();
        long total;
        if (result instanceof Number) {
            total = ((Number) result).longValue();
        } else {
            total = Long.parseLong(result.toString());
        }
        
        return new PageImpl<>(content, pageable, total);
    }
}
