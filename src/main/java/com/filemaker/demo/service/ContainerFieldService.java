package com.filemaker.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;

/**
 * Service for handling FileMaker container fields (binary data).
 * <p>
 * FileMaker container fields require special SQL syntax:
 * <ul>
 *   <li><b>Read:</b> {@code SELECT GetAs(field, 'format') FROM table WHERE id = ?}</li>
 *   <li><b>Write:</b> {@code UPDATE table SET field = ? AS 'filename.ext' WHERE id = ?}</li>
 * </ul>
 * <p>
 * Standard JPA/Hibernate @Lob mapping does NOT work with FileMaker containers.
 * This service uses native JDBC to handle the special syntax requirements.
 *
 * @author FileMaker Hibernate Dialect
 */
@Service
public class ContainerFieldService {

    private static final Logger log = LoggerFactory.getLogger(ContainerFieldService.class);

    private final DataSource dataSource;

    public ContainerFieldService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Upload binary data to a FileMaker container field.
     *
     * @param tableName  The table name
     * @param fieldName  The container field name
     * @param recordId   The record ID
     * @param file       The file to upload
     * @return true if successful
     */
    public boolean uploadToContainer(String tableName, String fieldName, Long recordId, MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) {
            filename = "upload.bin";
        }

        // FileMaker requires: UPDATE table SET container = ? AS 'filename.ext' WHERE id = ?
        String sql = String.format(
            "UPDATE %s SET %s = ? AS '%s' WHERE id = ?",
            tableName, fieldName, filename
        );

        log.info("Uploading to container: {} bytes to {}.{} for record {}", 
                 file.getSize(), tableName, fieldName, recordId);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // FileMaker requires setBytes(), not setBinaryStream()
            ps.setBytes(1, file.getBytes());
            ps.setLong(2, recordId);
            int updated = ps.executeUpdate();
            log.info("Container upload result: {} rows updated", updated);
            return updated > 0;

        } catch (SQLException | IOException e) {
            log.error("Failed to upload to container field: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Upload raw bytes to a FileMaker container field.
     *
     * @param tableName  The table name
     * @param fieldName  The container field name
     * @param recordId   The record ID
     * @param data       The binary data
     * @param filename   The filename (used to determine format)
     * @return true if successful
     */
    public boolean uploadToContainer(String tableName, String fieldName, Long recordId, 
                                     byte[] data, String filename) {
        if (filename == null || filename.isEmpty()) {
            filename = "upload.bin";
        }

        String sql = String.format(
            "UPDATE %s SET %s = ? AS '%s' WHERE id = ?",
            tableName, fieldName, filename
        );

        log.info("Uploading {} bytes to {}.{} for record {}", 
                 data.length, tableName, fieldName, recordId);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBytes(1, data);
            ps.setLong(2, recordId);
            int updated = ps.executeUpdate();
            log.info("Container upload result: {} rows updated", updated);
            return updated > 0;

        } catch (SQLException e) {
            log.error("Failed to upload to container field: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Download binary data from a FileMaker container field.
     * <p>
     * FileMaker uses 4-character type codes (classic Mac OS style):
     * <ul>
     *   <li>{@code GIFf} - Graphics Interchange Format</li>
     *   <li>{@code JPEG} - Photographic images</li>
     *   <li>{@code TIFF} - Raster file format for digital images</li>
     *   <li>{@code PDF } - Portable Document Format (note trailing space)</li>
     *   <li>{@code PNGf} - Bitmap image format (PNG)</li>
     * </ul>
     * Note: {@code FILE} format returns NULL for typed content; use the specific type code.
     *
     * @param tableName  The table name
     * @param fieldName  The container field name
     * @param recordId   The record ID
     * @param format     The format to retrieve (e.g., "JPEG", "PNGf", "TIFF")
     * @return The binary data, or null if not found or empty
     */
    public byte[] downloadFromContainer(String tableName, String fieldName, Long recordId, String format) {
        // Convert common format names to FileMaker type codes
        String fmFormat = toFileMakerTypeCode(format);
        
        // FileMaker requires: SELECT GetAs(field, 'format') FROM table WHERE id = ?
        String sql = String.format(
            "SELECT GetAs(%s, '%s') FROM %s WHERE id = ?",
            fieldName, fmFormat, tableName
        );

        log.info("Downloading from container: {}.{} for record {} as {}", 
                 tableName, fieldName, recordId, format);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, recordId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    byte[] data = rs.getBytes(1);
                    if (data != null) {
                        log.info("Downloaded {} bytes from container", data.length);
                    } else {
                        log.info("Container field is empty or format not available");
                    }
                    return data;
                }
            }

        } catch (SQLException e) {
            log.error("Failed to download from container field: {}", e.getMessage(), e);
        }

        return null;
    }

    /**
     * Download binary data from a FileMaker container field using FILE format.
     * This retrieves the raw file data regardless of type.
     *
     * @param tableName  The table name
     * @param fieldName  The container field name
     * @param recordId   The record ID
     * @return The binary data, or null if not found or empty
     */
    public byte[] downloadFromContainer(String tableName, String fieldName, Long recordId) {
        return downloadFromContainer(tableName, fieldName, recordId, "FILE");
    }

    /**
     * Clear a container field (set to NULL).
     *
     * @param tableName  The table name
     * @param fieldName  The container field name
     * @param recordId   The record ID
     * @return true if successful
     */
    public boolean clearContainer(String tableName, String fieldName, Long recordId) {
        String sql = String.format(
            "UPDATE %s SET %s = NULL WHERE id = ?",
            tableName, fieldName
        );

        log.info("Clearing container: {}.{} for record {}", tableName, fieldName, recordId);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, recordId);
            int updated = ps.executeUpdate();
            log.info("Container clear result: {} rows updated", updated);
            return updated > 0;

        } catch (SQLException e) {
            log.error("Failed to clear container field: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get the file reference (path) from a container field.
     * Uses CAST to VARCHAR to retrieve the file path/reference.
     *
     * @param tableName  The table name
     * @param fieldName  The container field name
     * @param recordId   The record ID
     * @return The file reference string, or null if not found
     */
    public String getContainerReference(String tableName, String fieldName, Long recordId) {
        String sql = String.format(
            "SELECT CAST(%s AS VARCHAR) FROM %s WHERE id = ?",
            fieldName, tableName
        );

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, recordId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }

        } catch (SQLException e) {
            log.error("Failed to get container reference: {}", e.getMessage(), e);
        }

        return null;
    }

    /**
     * Convert common format names to FileMaker 4-character type codes.
     * FileMaker uses classic Mac OS type codes for container data.
     * <p>
     * <b>Supported formats:</b>
     * <ul>
     *   <li>{@code GIFf} - Graphics Interchange Format</li>
     *   <li>{@code JPEG} - Photographic images</li>
     *   <li>{@code TIFF} - Raster file format for digital images</li>
     *   <li>{@code PDF } - Portable Document Format (trailing space required!)</li>
     *   <li>{@code PNGf} - Bitmap image format (PNG)</li>
     * </ul>
     *
     * @param format The format name (e.g., "PNG", "JPEG", "PDF")
     * @return The FileMaker type code
     */
    private String toFileMakerTypeCode(String format) {
        if (format == null) {
            return "PDF ";  // Default to PDF with trailing space
        }
        return switch (format.toUpperCase()) {
            case "PNG" -> "PNGf";
            case "PNGF" -> "PNGf";
            case "JPG", "JPEG" -> "JPEG";
            case "GIF" -> "GIFf";
            case "GIFF" -> "GIFf";
            case "TIFF", "TIF" -> "TIFF";
            case "PDF" -> "PDF ";  // Trailing space is REQUIRED
            case "PDF " -> "PDF "; // Already correct
            case "FILE" -> "FILE";
            default -> format;  // Pass through if already a type code
        };
    }
}
