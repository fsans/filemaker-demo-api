# Testing Guide for FileMaker Demo API

This document provides comprehensive information about the test suite for the FileMaker Demo API.

## Overview

The test suite provides complete coverage for all API endpoints and business logic while ensuring no existing data in the FileMaker database is modified or deleted during testing.

## Test Structure

```text
src/test/java/com/filemaker/demo/
├── controller/
│   ├── ContactControllerTest.java     # Tests for all contact CRUD endpoints
│   └── PhotoControllerTest.java       # Tests for photo upload/download endpoints
├── repository/
│   └── ContactRepositoryTest.java     # Tests for data access layer
├── service/
│   └── ContainerFieldServiceTest.java # Tests for FileMaker container field operations
├── integration/
│   └── ContactIntegrationTest.java    # End-to-end workflow tests
└── TestSuite.java                      # Test suite runner

src/test/resources/
└── application-test.properties        # Test configuration
```

## Test Configuration

### Database Configuration

Tests use **FileMaker database** to ensure realistic testing against the actual database:

```properties
# Test Configuration (application-test.properties)
spring.datasource.url=jdbc:filemaker://192.168.0.24/Contacts
spring.datasource.driver-class-name=com.filemaker.jdbc.Driver
spring.jpa.database-platform=org.hibernate.community.dialect.FileMakerDialect
spring.jpa.hibernate.ddl-auto=none
```

## Running Tests

### Run All Tests

```bash
# Using Maven
mvn test

# Using Maven with specific profile
mvn test -Dspring.profiles.active=filemaker-test

# Run specific test class
mvn test -Dtest=ContactControllerTest

# Run specific test method
mvn test -Dtest=ContactControllerTest#testCreateContact
```

### Run Test Suite

```bash
# Run the complete test suite
mvn test -Dtest=TestSuite
```

### Run with Coverage Report

```bash
# Generate test coverage report
mvn clean test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

## Test Coverage

### Contact Management Endpoints

| Endpoint | Method | Test Coverage |
|----------|--------|---------------|
| `/api/contacts` | GET | ✅ Pagination, sorting, empty results |
| `/api/contacts/{id}` | GET | ✅ Found, not found scenarios |
| `/api/contacts/search` | GET | ✅ Search by name, email, company with pagination |
| `/api/contacts/by-company/{company}` | GET | ✅ Company filtering, empty results |
| `/api/contacts` | POST | ✅ Valid creation, validation errors |
| `/api/contacts/{id}` | PUT | ✅ Full update, not found |
| `/api/contacts/{id}` | PATCH | ✅ Partial update, not found |
| `/api/contacts/{id}` | DELETE | ✅ Safe deletion, not found |

### Photo Management Endpoints

| Endpoint | Method | Test Coverage |
|----------|--------|---------------|
| `/api/contacts/{id}/photo` | POST | ✅ Upload, validation, file types |
| `/api/contacts/{id}/photo` | GET | ✅ Download, formats, not found |
| `/api/contacts/{id}/photo/inline` | GET | ✅ Inline viewing, content types |
| `/api/contacts/{id}/photo` | DELETE | ✅ Deletion, not found |
| `/api/contacts/{id}/photo/info` | GET | ✅ Metadata, availability check |

### Data Layer Tests

- **Repository Operations**: CRUD, pagination, sorting, searching
- **Custom Queries**: Company filtering, text search
- **FileMaker Integration**: Auto-enter fields, read-only properties
- **Transaction Management**: Rollback behavior

### Service Layer Tests

- **Container Field Operations**: Upload, download, delete
- **Format Handling**: JPEG, PNG, GIF, TIFF, PDF
- **Error Scenarios**: Invalid records, permissions, file corruption
- **Performance**: Large file handling, concurrent operations

### Integration Tests

- **Complete Workflows**: Create → Update → Search → Delete
- **Error Handling**: 404 scenarios, validation failures
- **Photo Integration**: Upload → Info → Download → Delete
- **Search & Filter**: Complex search scenarios with pagination

## Test Data Management

### Safe Testing Principles

1. **No Production Data Impact**: Tests use dedicated FileMaker test database
2. **Isolated Test Data**: Each test creates its own data
3. **Transactional Cleanup**: `@Transactional` ensures rollback after each test
4. **Mocked Dependencies**: Container field operations are mocked in controller tests
5. **Safe Deletion**: Delete operations only affect test-created records

### Test Data Creation

```java
// Example: Test contact creation
Contact testContact = new Contact();
testContact.setEmail("test@example.com");
testContact.setLogin("testuser");
testContact.setPassword("testpass");
testContact.setFirstName("Test");
testContact.setLastName("User");
testContact = contactRepository.save(testContact);
```

### Cleanup Strategy

- **Automatic**: `@Transactional` annotation rolls back changes after each test
- **Manual**: Integration tests explicitly delete created records
- **Isolation**: Each test method runs in a separate transaction

## Mocking Strategy

### Container Field Service

```java
@MockBean
private ContainerFieldService containerFieldService;

// Mock upload operation
when(containerFieldService.uploadToContainer(anyString(), anyString(), anyLong(), any()))
    .thenReturn(true);

// Mock download operation
when(containerFieldService.downloadFromContainer(anyString(), anyString(), anyLong(), anyString()))
    .thenReturn(testImageData);
```

### Benefits of Mocking

1. **Isolation**: Tests don't depend on FileMaker container field availability
2. **Speed**: No actual file I/O operations during testing
3. **Reliability**: Consistent test results regardless of FileMaker server state
4. **Coverage**: All error scenarios can be tested

## Performance Testing

### Large File Handling

```java
@Test
void testUploadLargeData() {
    // Create 1MB test data
    byte[] largeData = new byte[1024 * 1024];
    // Test upload performance and memory usage
}
```

### Concurrent Operations

```java
@Test
void testConcurrentUploads() throws InterruptedException {
    // Test multiple simultaneous uploads
    ExecutorService executor = Executors.newFixedThreadPool(10);
    // Submit multiple upload tasks
}
```

## Error Scenario Testing

### Network Failures

```java
@Test
void testFileMakerConnectionFailure() {
    // Test behavior when FileMaker is unavailable
    when(containerFieldService.uploadToContainer(...))
        .thenThrow(new SQLException("Connection failed"));
}
```

### Data Validation

```java
@Test
void testCreateContact_InvalidInput() throws Exception {
    ContactDTO invalidContact = new ContactDTO();
    // Missing required fields
    mockMvc.perform(post("/api/contacts")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(invalidContact)))
        .andExpect(status().isBadRequest());
}
```

## Continuous Integration

### GitHub Actions Example

```yaml
name: Test Suite
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 17
        uses: actions/setup-java@v2
        with:
          java-version: '17'
          distribution: 'adopt'
      - name: Run tests
        run: mvn clean test
      - name: Generate coverage report
        run: mvn jacoco:report
      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v1
```

## Best Practices

### Test Naming

```java
// Good: Descriptive test names
void testCreateContact_ValidInput_ReturnsCreatedContact()
void testGetContactById_NotFound_Returns404()

// Bad: Generic test names
void test1()
void testContact()
```

### Test Organization

```java
@Test
void testCreateContact() {
    // Arrange
    ContactDTO newContact = createTestContact();
    
    // Act
    MvcResult result = mockMvc.perform(post("/api/contacts")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(newContact)))
        .andReturn();
    
    // Assert
    mockMvc.perform(asyncDispatch(result))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email", equalTo(newContact.getEmail())));
}
```

### Assertion Strategies

```java
// Use specific assertions
assertThat(response.getEmail()).isEqualTo(expectedEmail);
assertThat(response.getId()).isNotNull();

// Use Hamcrest matchers for JSON
.andExpect(jsonPath("$.email", equalTo("test@example.com")))
.andExpect(jsonPath("$.id", notNullValue()))
.andExpect(jsonPath("$.content", hasSize(greaterThan(0))));
```

## Troubleshooting

### Common Issues

1. **FileMaker Connection Errors**: Check FileMaker server availability and network connectivity
2. **Test Data Conflicts**: Ensure `@Transactional` is used on test classes
3. **Mock Not Working**: Verify `@MockBean` is properly configured
4. **Container Field Tests**: These are mocked in controller tests, use integration tests for real FileMaker testing

### FileMaker Database Setup

Ensure your FileMaker test database is properly configured:

```properties
# Required FileMaker configuration
spring.datasource.url=jdbc:filemaker://192.168.0.24/Contacts
spring.datasource.driver-class-name=com.filemaker.jdbc.Driver
spring.datasource.username=admin
spring.datasource.password=wakawaka
spring.jpa.database-platform=org.hibernate.community.dialect.FileMakerDialect
```

### Debug Mode

Enable debug logging in test properties:

```properties
logging.level.com.filemaker.demo=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.hibernate.SQL=DEBUG
```

## Coverage Reports

Generate and view coverage reports:

```bash
mvn clean test jacoco:report
open target/site/jacoco/index.html
```

Target coverage goals:

- **Controller Tests**: 100% endpoint coverage
- **Service Tests**: 90%+ business logic coverage
- **Repository Tests**: 85%+ data access coverage
- **Overall**: 85%+ code coverage

## Contributing

When adding new features:

1. **Write Tests First**: TDD approach ensures testable code
2. **Cover All Scenarios**: Happy path, error cases, edge cases
3. **Update Documentation**: Keep this testing guide current
4. **Run Full Suite**: Ensure all tests pass before submitting
5. **Check Coverage**: Maintain or improve overall coverage percentage

## Future Enhancements

Planned test improvements:

1. **Performance Tests**: Load testing for high-volume scenarios
2. **Security Tests**: Authentication and authorization testing
3. **Contract Tests**: API contract validation with Pact
4. **Chaos Tests**: Failure injection and resilience testing
5. **Browser Tests**: Selenium-based UI testing for Swagger interface
