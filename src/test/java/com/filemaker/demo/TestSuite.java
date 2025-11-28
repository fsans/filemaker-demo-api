package com.filemaker.demo;

import com.filemaker.demo.controller.ContactControllerTest;
import com.filemaker.demo.controller.PhotoControllerTest;
import com.filemaker.demo.integration.ContactIntegrationTest;
import com.filemaker.demo.repository.ContactRepositoryTest;
import com.filemaker.demo.service.ContainerFieldServiceTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Test Suite for FileMaker Demo API
 * 
 * This test suite runs all test classes in the following order:
 * 1. Repository tests (data layer)
 * 2. Service tests (business logic)
 * 3. Controller tests (API endpoints)
 * 4. Integration tests (end-to-end workflows)
 * 
 * Tests are designed to use FileMaker test database to ensure realistic
 * testing while avoiding impact on production data. Each test uses @Transactional
 * to ensure cleanup after execution.
 * 
 * Key testing principles:
 * - Tests create their own data and don't rely on existing records
 * - Delete operations only affect test-created records
 * - Container field operations are mocked for controller tests
 * - Integration tests verify complete workflows
 */
@Suite
@SelectClasses({
    // Data Layer Tests
    ContactRepositoryTest.class,
    
    // Service Layer Tests
    ContainerFieldServiceTest.class,
    
    // Controller Layer Tests
    ContactControllerTest.class,
    PhotoControllerTest.class,
    
    // Integration Tests
    ContactIntegrationTest.class
})
public class TestSuite {
    // Test suite class - no implementation needed
    // JUnit Platform Suite will automatically discover and run all selected tests
}
