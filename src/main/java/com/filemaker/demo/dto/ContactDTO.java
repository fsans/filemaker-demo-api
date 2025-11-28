package com.filemaker.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for creating/updating contacts.
 * Excludes read-only fields like id, uuid, timestamps.
 */
@Schema(description = "Contact data for create/update operations")
public class ContactDTO {

    @Schema(description = "Email address", example = "john.doe@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "Login username", example = "johndoe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String login;

    @Schema(description = "Password", example = "secret123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "First name", example = "John")
    private String firstName;

    @Schema(description = "Last name", example = "Doe")
    private String lastName;

    @Schema(description = "Title (Mr, Mrs, Dr, etc)", example = "Mr")
    private String title;

    @Schema(description = "Job title", example = "Software Engineer")
    private String jobTitle;

    @Schema(description = "Company name", example = "Acme Corp")
    private String company;

    @Schema(description = "Website URL", example = "https://example.com")
    private String website;

    @Schema(description = "Notes or comments")
    private String notes;

    // Getters and Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
