package org.example.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ImageStorageService}.
 * 
 * <p>
 * Following FIRST principles with Given/When/Then format.
 * </p>
 * <p>
 * Uses @TempDir for isolated file system testing.
 * </p>
 */
class ImageStorageServiceTest {

    private ImageStorageService imageStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        imageStorageService = new ImageStorageService();
        ReflectionTestUtils.setField(imageStorageService, "imageStoragePath", tempDir.toString());
        ReflectionTestUtils.setField(imageStorageService, "imageBaseUrl", "http://localhost:8080/images");
    }

    @Nested
    @DisplayName("saveImage")
    class SaveImageTests {

        @Test
        @DisplayName("should save image and return public URL")
        void shouldSaveImageAndReturnUrl() {
            // Given
            byte[] imageBytes = "fake-image-data".getBytes();
            String format = "jpg";
            LocalDateTime timestamp = LocalDateTime.of(2025, 12, 21, 10, 30, 0);
            String deviceId = "device-123";

            // When
            String result = imageStorageService.saveImage(imageBytes, format, timestamp, deviceId);

            // Then
            assertThat(result).startsWith("http://localhost:8080/images/2025-12-21/");
            assertThat(result).contains("device-123");
            assertThat(result).endsWith(".jpg");
        }

        @Test
        @DisplayName("should create date-based subdirectory")
        void shouldCreateDateBasedSubdirectory() {
            // Given
            byte[] imageBytes = "test-image".getBytes();
            LocalDateTime timestamp = LocalDateTime.of(2025, 6, 15, 12, 0, 0);

            // When
            imageStorageService.saveImage(imageBytes, "png", timestamp, "device-1");

            // Then
            Path dateDir = tempDir.resolve("2025-06-15");
            assertThat(Files.exists(dateDir)).isTrue();
            assertThat(Files.isDirectory(dateDir)).isTrue();
        }

        @Test
        @DisplayName("should write image bytes to file")
        void shouldWriteImageBytesToFile() throws Exception {
            // Given
            byte[] imageBytes = "test-image-content".getBytes();
            LocalDateTime timestamp = LocalDateTime.of(2025, 1, 1, 0, 0, 0);

            // When
            String url = imageStorageService.saveImage(imageBytes, "jpg", timestamp, "test-device");

            // Then
            Path dateDir = tempDir.resolve("2025-01-01");
            Path[] savedFiles = Files.list(dateDir).toArray(Path[]::new);
            assertThat(savedFiles).hasSize(1);

            byte[] savedContent = Files.readAllBytes(savedFiles[0]);
            assertThat(savedContent).isEqualTo(imageBytes);
        }

        @Test
        @DisplayName("should generate unique filenames for same timestamp")
        void shouldGenerateUniqueFilenames() {
            // Given
            byte[] imageBytes1 = "image1".getBytes();
            byte[] imageBytes2 = "image2".getBytes();
            LocalDateTime timestamp = LocalDateTime.of(2025, 1, 1, 12, 0, 0);

            // When
            String url1 = imageStorageService.saveImage(imageBytes1, "jpg", timestamp, "device-1");
            String url2 = imageStorageService.saveImage(imageBytes2, "jpg", timestamp, "device-1");

            // Then
            assertThat(url1).isNotEqualTo(url2);
        }
    }

    @Nested
    @DisplayName("deleteImage")
    class DeleteImageTests {

        @Test
        @DisplayName("should delete existing image and return true")
        void shouldDeleteExistingImageAndReturnTrue() throws Exception {
            // Given
            byte[] imageBytes = "test-image".getBytes();
            LocalDateTime timestamp = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
            String url = imageStorageService.saveImage(imageBytes, "jpg", timestamp, "device-1");

            // When
            boolean result = imageStorageService.deleteImage(url);

            // Then
            assertThat(result).isTrue();

            Path dateDir = tempDir.resolve("2025-01-01");
            long fileCount = Files.list(dateDir).count();
            assertThat(fileCount).isZero();
        }

        @Test
        @DisplayName("should return false when image does not exist")
        void shouldReturnFalseWhenImageNotFound() {
            // Given
            String nonExistentUrl = "http://localhost:8080/images/2025-01-01/nonexistent.jpg";

            // When
            boolean result = imageStorageService.deleteImage(nonExistentUrl);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getLocalImagePath")
    class GetLocalImagePathTests {

        @Test
        @DisplayName("should convert URL to local path correctly")
        void shouldConvertUrlToLocalPath() {
            // Given
            String imageUrl = "http://localhost:8080/images/2025-12-21/image_123.jpg";

            // When
            Path result = imageStorageService.getLocalImagePath(imageUrl);

            // Then
            assertThat(result.toString()).contains("2025-12-21");
            assertThat(result.toString()).contains("image_123.jpg");
        }
    }
}
