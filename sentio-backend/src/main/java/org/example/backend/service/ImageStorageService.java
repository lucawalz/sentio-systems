package org.example.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Service responsible for image file storage and management operations.
 * Handles saving, deleting, and retrieving image files with organized directory structure.
 * <p>
 * This service implements date-based directory organization and generates unique
 * filenames to prevent conflicts. Images are stored locally with configurable
 * storage path and accessible via configurable base URL.
 * </p>
 *
 * @author Sentio Team
 * @version 1.0
 * @since 1.0
 */
@Service
@Slf4j
public class ImageStorageService {

    @Value("${app.image.storage.path:./images}")
    private String imageStoragePath;

    @Value("${app.image.base-url:http://localhost:8080/images}")
    private String imageBaseUrl;

    /**
     * Converts a public image URL to its corresponding local file path.
     *
     * @param imageUrl The public URL of the image
     * @return Path object pointing to the local file location
     */
    public Path getLocalImagePath(String imageUrl) {
        log.debug("Converting image URL to local path: {}", imageUrl);
        String relativePath = imageUrl.replace(imageBaseUrl + "/", "");
        return Paths.get(imageStoragePath, relativePath);
    }

    /**
     * Saves an image file to the storage system with organized directory structure.
     * Creates date-based subdirectories and generates unique filenames to prevent conflicts.
     *
     * @param imageBytes Binary data of the image
     * @param format     Image file format (e.g., "jpg", "png")
     * @param timestamp  Timestamp for filename generation and directory organization
     * @param deviceId   Device identifier for filename uniqueness
     * @return Public URL for accessing the saved image
     * @throws RuntimeException if image saving fails
     */
    public String saveImage(byte[] imageBytes, String format, LocalDateTime timestamp, String deviceId) {
        log.debug("Saving image - Format: {}, Size: {} bytes, Device: {}", format, imageBytes.length, deviceId);

        try {
            // Create storage directory if it doesn't exist
            Path storageDir = Paths.get(imageStoragePath);
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
                log.info("Created image storage directory: {}", storageDir.toAbsolutePath());
            }

            // Generate filename with timestamp and device ID
            String datePrefix = timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String timePrefix = timestamp.format(DateTimeFormatter.ofPattern("HH-mm-ss"));
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);
            String fileName = String.format("%s_%s_%s_%s.%s",
                    datePrefix, timePrefix, deviceId, uniqueId, format);

            // Create date-based subdirectory
            Path dateDir = storageDir.resolve(datePrefix);
            if (!Files.exists(dateDir)) {
                Files.createDirectories(dateDir);
                log.debug("Created date directory: {}", dateDir);
            }

            Path filePath = dateDir.resolve(fileName);
            Files.write(filePath, imageBytes);

            String publicUrl = String.format("%s/%s/%s", imageBaseUrl, datePrefix, fileName);

            log.info("Successfully saved image: {} (size: {} bytes)", fileName, imageBytes.length);
            return publicUrl;

        } catch (IOException e) {
            log.error("Failed to save image - Format: {}, Size: {} bytes, Device: {}",
                    format, imageBytes.length, deviceId, e);
            throw new RuntimeException("Failed to save image", e);
        }
    }

    /**
     * Deletes an image file from the storage system.
     *
     * @param imageUrl The public URL of the image to delete
     * @return true if image was successfully deleted, false if file not found or deletion failed
     */
    public boolean deleteImage(String imageUrl) {
        log.debug("Attempting to delete image: {}", imageUrl);

        try {
            // Extract relative path from URL
            String relativePath = imageUrl.replace(imageBaseUrl + "/", "");
            Path filePath = Paths.get(imageStoragePath, relativePath);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Successfully deleted image: {}", relativePath);
                return true;
            } else {
                log.warn("Image file not found for deletion: {}", relativePath);
                return false;
            }
        } catch (IOException e) {
            log.error("Failed to delete image: {}", imageUrl, e);
            return false;
        }
    }
}