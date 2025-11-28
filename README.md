# FileMaker Demo API

REST API demo for testing the FileMaker Hibernate Dialect with Spring Boot and HikariCP.

## Prerequisites

1. **FileMaker JDBC Driver** installed in local Maven repo:
   ```bash
   cd ../FileMakerHibernate6
   ./maven_deploy_driver.sh 21.0.2
   ```

2. **FileMaker Dialect** installed in local Maven repo:
   ```bash
   cd ../FileMakerHibernate6
   ./maven_deploy_dialect.sh 21.0.2
   ```

3. **FileMaker Server** running with the `Contacts` database accessible

## Configuration

Edit `src/main/resources/application.yml` to set your FileMaker connection:

```yaml
spring:
  datasource:
    url: jdbc:filemaker://YOUR_HOST/YOUR_DATABASE
    username: YOUR_USERNAME
    password: YOUR_PASSWORD
```

## Running the Application

```bash
mvn spring-boot:run
```

## API Endpoints

Once running, access:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api-docs

### Available Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/contacts` | List all contacts (paginated) |
| GET | `/api/contacts/{id}` | Get contact by ID |
| GET | `/api/contacts/search?q=` | Search contacts |
| GET | `/api/contacts/by-company/{company}` | Get contacts by company |
| POST | `/api/contacts` | Create new contact |
| PUT | `/api/contacts/{id}` | Update contact |
| PATCH | `/api/contacts/{id}` | Partial update |
| DELETE | `/api/contacts/{id}` | Delete contact |

## Testing with Postman

Import the Swagger spec from `http://localhost:8080/api-docs` into Postman for easy testing.

## Stack

- Spring Boot 3.2
- Spring Data JPA
- HikariCP (connection pool)
- FileMaker Hibernate Dialect
- SpringDoc OpenAPI (Swagger)
