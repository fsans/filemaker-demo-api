package com.filemaker.demo.entity;

import jakarta.persistence.*;
import org.hibernate.community.dialect.entity.FileMakerBaseEntity;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * Contact entity mapped to FileMaker 'contact' table.
 * Extends FileMakerBaseEntity to leverage FileMaker-specific ID generation
 * and best practices for Hibernate integration.
 */
@Entity
@Table(name = "contact")
public class Contact extends FileMakerBaseEntity {

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String login;

    @Column(nullable = false)
    private String password;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    private String title;

    @Column(name = "job_title")
    private String jobTitle;

    private String company;

    private String website;

    @Column(length = 100000)
    private String notes;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "photo_content_type")
    private String photoContentType;

    // Note: photo_content is a FileMaker container field (BLOB)
    // It requires special handling via native SQL with GetAs()/PutAs() functions
    // Do NOT map it as a regular @Lob field - use ContainerFieldService instead

    @Column(name = "last_contact_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastContactDate;

    // FileMaker system columns - commented out due to case sensitivity issues
    // @Column(name = "ROWID", insertable = false, updatable = false)
    // private Long recordId;

    // @Column(name = "ROWMODID", insertable = false, updatable = false)
    // private Integer modificationCount;

    // FileMaker auto-enter fields (read-only)
    @Column(insertable = false, updatable = false)
    private String uuid;

    @Column(insertable = false, updatable = false)
    private String sku;

    @Column(name = "create_timestamp", insertable = false, updatable = false)
    private LocalDateTime createTimestamp;

    @Column(name = "update_timestamp", insertable = false, updatable = false)
    private LocalDateTime updateTimestamp;

    // Constructors
    public Contact() {}

    public Contact(String email, String login, String password) {
        this.email = email;
        this.login = login;
        this.password = password;
    }

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

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getPhotoContentType() { return photoContentType; }
    public void setPhotoContentType(String photoContentType) { this.photoContentType = photoContentType; }

    public Date getLastContactDate() { return lastContactDate; }
    public void setLastContactDate(Date lastContactDate) { this.lastContactDate = lastContactDate; }

    // public Long getRecordId() { return recordId; }
    // public Integer getModificationCount() { return modificationCount; }
    public String getUuid() { return uuid; }
    public String getSku() { return sku; }
    public LocalDateTime getCreateTimestamp() { return createTimestamp; }
    public LocalDateTime getUpdateTimestamp() { return updateTimestamp; }
}
