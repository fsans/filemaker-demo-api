package com.filemaker.demo.controller;

import com.filemaker.demo.repository.ContactRepository;
import com.filemaker.demo.service.ContainerFieldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller for handling contact photo uploads and downloads.
 * <p>
 * FileMaker container fields require special SQL syntax that standard JPA cannot handle.
 * This controller uses {@link ContainerFieldService} to manage binary data via native JDBC.
 */
@RestController
@RequestMapping("/api/contacts/{id}/photo")
@Tag(name = "Contact Photos", description = "Upload and download contact photos (FileMaker container field)")
public class PhotoController {

    private static final String TABLE_NAME = "contact";
    private static final String FIELD_NAME = "photo_content";

    private final ContainerFieldService containerFieldService;
    private final ContactRepository contactRepository;

    public PhotoController(ContainerFieldService containerFieldService, 
                          ContactRepository contactRepository) {
        this.containerFieldService = containerFieldService;
        this.contactRepository = contactRepository;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload photo", description = "Upload a photo to a contact's container field")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Photo uploaded successfully"),
        @ApiResponse(responseCode = "404", description = "Contact not found"),
        @ApiResponse(responseCode = "400", description = "Invalid file or upload failed")
    })
    public ResponseEntity<String> uploadPhoto(
            @Parameter(description = "Contact ID") @PathVariable Long id,
            @Parameter(description = "Photo file") @RequestParam("file") MultipartFile file
    ) {
        // Verify contact exists
        if (!contactRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        boolean success = containerFieldService.uploadToContainer(TABLE_NAME, FIELD_NAME, id, file);

        if (success) {
            // Also update the content type field
            contactRepository.findById(id).ifPresent(contact -> {
                contact.setPhotoContentType(file.getContentType());
                contactRepository.save(contact);
            });
            return ResponseEntity.ok("Photo uploaded successfully");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload photo");
        }
    }

    @GetMapping
    @Operation(summary = "Download photo", description = "Download a contact's photo from the container field")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Photo retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Contact not found or no photo available")
    })
    public ResponseEntity<byte[]> downloadPhoto(
            @Parameter(description = "Contact ID") @PathVariable Long id,
            @Parameter(description = "Image format (JPEG, PNGf, GIFf, PDF, TIFF)") 
            @RequestParam(required = false) String format
    ) {
        // Verify contact exists
        var contactOpt = contactRepository.findById(id);
        if (contactOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Auto-detect format from content type if not specified
        String effectiveFormat = format;
        if (effectiveFormat == null || effectiveFormat.isEmpty()) {
            String storedContentType = contactOpt.get().getPhotoContentType();
            effectiveFormat = detectFormatFromContentType(storedContentType);
        }

        // Download - use probing if format is still unknown
        byte[] photoData;
        if (effectiveFormat == null) {
            // Content type unknown (e.g., data entered from FileMaker without setting content type)
            photoData = downloadWithAutoDetect(id);
        } else {
            photoData = containerFieldService.downloadFromContainer(TABLE_NAME, FIELD_NAME, id, effectiveFormat);
        }

        if (photoData == null || photoData.length == 0) {
            return ResponseEntity.notFound().build();
        }

        // Determine content type for response
        // If format was explicitly specified, use that format's content type
        // Otherwise use stored content type or derive from effective format
        String contentType;
        if (format != null && !format.isEmpty()) {
            // User explicitly requested a format - use that format's content type
            contentType = determineContentType(format);
        } else {
            contentType = contactOpt.get().getPhotoContentType();
            if (contentType == null || contentType.isEmpty()) {
                contentType = determineContentType(effectiveFormat);
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentLength(photoData.length);
        headers.setContentDispositionFormData("attachment", "photo." + getExtension(effectiveFormat));

        return new ResponseEntity<>(photoData, headers, HttpStatus.OK);
    }

    @GetMapping("/inline")
    @Operation(summary = "View photo inline", description = "View a contact's photo in the browser")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Photo retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Contact not found or no photo available")
    })
    public ResponseEntity<byte[]> viewPhoto(
            @Parameter(description = "Contact ID") @PathVariable Long id,
            @Parameter(description = "Image format (JPEG, PNGf, GIFf, PDF, TIFF)") 
            @RequestParam(required = false) String format
    ) {
        var contactOpt = contactRepository.findById(id);
        if (contactOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Auto-detect format from content type if not specified
        String effectiveFormat = format;
        if (effectiveFormat == null || effectiveFormat.isEmpty()) {
            String storedContentType = contactOpt.get().getPhotoContentType();
            effectiveFormat = detectFormatFromContentType(storedContentType);
        }

        // Download - use probing if format is still unknown
        byte[] photoData;
        if (effectiveFormat == null) {
            photoData = downloadWithAutoDetect(id);
        } else {
            photoData = containerFieldService.downloadFromContainer(TABLE_NAME, FIELD_NAME, id, effectiveFormat);
        }

        if (photoData == null || photoData.length == 0) {
            return ResponseEntity.notFound().build();
        }

        // Determine content type for response
        String contentType;
        if (format != null && !format.isEmpty()) {
            contentType = determineContentType(format);
        } else {
            contentType = contactOpt.get().getPhotoContentType();
            if (contentType == null || contentType.isEmpty()) {
                contentType = determineContentType(effectiveFormat);
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentLength(photoData.length);
        // Inline display instead of download
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline");

        return new ResponseEntity<>(photoData, headers, HttpStatus.OK);
    }

    @DeleteMapping
    @Operation(summary = "Delete photo", description = "Remove a contact's photo from the container field")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Photo deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Contact not found")
    })
    public ResponseEntity<Void> deletePhoto(
            @Parameter(description = "Contact ID") @PathVariable Long id
    ) {
        if (!contactRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        boolean success = containerFieldService.clearContainer(TABLE_NAME, FIELD_NAME, id);

        if (success) {
            // Clear the content type field
            contactRepository.findById(id).ifPresent(contact -> {
                contact.setPhotoContentType(null);
                contactRepository.save(contact);
            });
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/info")
    @Operation(summary = "Get photo info", description = "Get information about the stored photo")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Photo info retrieved"),
        @ApiResponse(responseCode = "404", description = "Contact not found")
    })
    public ResponseEntity<PhotoInfo> getPhotoInfo(
            @Parameter(description = "Contact ID") @PathVariable Long id
    ) {
        var contactOpt = contactRepository.findById(id);
        if (contactOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String reference = containerFieldService.getContainerReference(TABLE_NAME, FIELD_NAME, id);
        String contentType = contactOpt.get().getPhotoContentType();

        PhotoInfo info = new PhotoInfo();
        info.setContactId(id);
        info.setContentType(contentType);
        info.setReference(reference);
        info.setHasPhoto(reference != null && !reference.isEmpty());

        return ResponseEntity.ok(info);
    }

    // Helper methods

    private String determineContentType(String format) {
        return switch (format.toUpperCase()) {
            case "JPEG", "JPG" -> "image/jpeg";
            case "PNG", "PNGF" -> "image/png";
            case "GIF", "GIFF" -> "image/gif";
            case "TIFF", "TIF" -> "image/tiff";
            case "PDF", "PDF " -> "application/pdf";
            default -> "application/octet-stream";
        };
    }

    private String getExtension(String format) {
        if (format == null) return "bin";
        return switch (format.toUpperCase()) {
            case "JPEG", "JPG" -> "jpg";
            case "PNG", "PNGF" -> "png";
            case "GIF", "GIFF" -> "gif";
            case "TIFF", "TIF" -> "tiff";
            case "PDF", "PDF " -> "pdf";
            default -> "bin";
        };
    }

    /**
     * Detect FileMaker format code from MIME content type.
     * If content type is unknown, returns null to trigger format probing.
     */
    private String detectFormatFromContentType(String contentType) {
        if (contentType == null || contentType.isEmpty()) {
            return null;  // Unknown - will trigger probing
        }
        return switch (contentType.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> "JPEG";
            case "image/png" -> "PNGf";
            case "image/gif" -> "GIFf";
            case "image/tiff" -> "TIFF";
            case "application/pdf" -> "PDF ";
            default -> null;  // Unknown - will trigger probing
        };
    }

    /**
     * Detect format from file reference (filename stored in container).
     * This is fast - single query vs probing multiple formats.
     */
    private String detectFormatFromReference(Long id) {
        String reference = containerFieldService.getContainerReference(TABLE_NAME, FIELD_NAME, id);
        if (reference == null || reference.isEmpty()) {
            return null;
        }
        
        String lowerRef = reference.toLowerCase();
        if (lowerRef.endsWith(".jpg") || lowerRef.endsWith(".jpeg")) {
            return "JPEG";
        } else if (lowerRef.endsWith(".png")) {
            return "PNGf";
        } else if (lowerRef.endsWith(".gif")) {
            return "GIFf";
        } else if (lowerRef.endsWith(".pdf")) {
            return "PDF ";
        } else if (lowerRef.endsWith(".tiff") || lowerRef.endsWith(".tif")) {
            return "TIFF";
        }
        return null;
    }

    /**
     * Download with smart format detection.
     * First tries to detect from file reference (fast), then falls back to probing (slow).
     */
    private byte[] downloadWithAutoDetect(Long id) {
        // First: try to detect from file reference (fast - single query)
        String format = detectFormatFromReference(id);
        if (format != null) {
            byte[] data = containerFieldService.downloadFromContainer(TABLE_NAME, FIELD_NAME, id, format);
            if (data != null && data.length > 0) {
                return data;
            }
        }
        
        // Fallback: probe formats (slower - multiple queries)
        String[] formatsToTry = {"JPEG", "PNGf", "PDF ", "GIFf", "TIFF"};
        for (String fmt : formatsToTry) {
            byte[] data = containerFieldService.downloadFromContainer(TABLE_NAME, FIELD_NAME, id, fmt);
            if (data != null && data.length > 0) {
                return data;
            }
        }
        return null;
    }

    // DTO for photo info
    public static class PhotoInfo {
        private Long contactId;
        private String contentType;
        private String reference;
        private boolean hasPhoto;

        public Long getContactId() { return contactId; }
        public void setContactId(Long contactId) { this.contactId = contactId; }

        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }

        public String getReference() { return reference; }
        public void setReference(String reference) { this.reference = reference; }

        public boolean isHasPhoto() { return hasPhoto; }
        public void setHasPhoto(boolean hasPhoto) { this.hasPhoto = hasPhoto; }
    }
}
