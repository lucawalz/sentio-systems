package dev.syslabs.sentio.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WeatherRadarMetadata Model")
class WeatherRadarMetadataTest {

    @Test
    @DisplayName("WeatherRadarMetadata should initialize with no-arg constructor")
    void weatherRadarMetadata_noArgConstructor_createsInstance() {
        // Act
        WeatherRadarMetadata metadata = new WeatherRadarMetadata();

        // Assert
        assertThat(metadata).isNotNull();
        assertThat(metadata.getId()).isNull();
    }

    @Test
    @DisplayName("WeatherRadarMetadata all-args constructor should set all fields")
    void weatherRadarMetadata_allArgsConstructor_setsAllFields() {
        // Arrange
        Long id = 1L;
        String deviceId = "device-123";
        LocalDateTime timestamp = LocalDateTime.of(2025, 1, 1, 12, 0);
        String source = "DWD_RV";
        Float latitude = 52.52F;
        Float longitude = 13.41F;
        Integer distance = 5000;
        Float precipMin = 0.0F;
        Float precipMax = 10.5F;
        Float precipAvg = 2.3F;
        Float coveragePercent = 45.7F;
        Integer significantRainCells = 120;
        Integer totalCells = 500;
        String geometryJson = "{\"type\":\"Polygon\",\"coordinates\":[...]}";
        String bboxPixels = "100,200,300,400";
        LocalDateTime createdAt = LocalDateTime.now();

        // Act
        WeatherRadarMetadata metadata = new WeatherRadarMetadata(
                id, deviceId, timestamp, source, latitude, longitude, distance,
                precipMin, precipMax, precipAvg, coveragePercent,
                significantRainCells, totalCells, geometryJson, bboxPixels, createdAt
        );

        // Assert
        assertThat(metadata.getId()).isEqualTo(id);
        assertThat(metadata.getDeviceId()).isEqualTo(deviceId);
        assertThat(metadata.getTimestamp()).isEqualTo(timestamp);
        assertThat(metadata.getSource()).isEqualTo(source);
        assertThat(metadata.getLatitude()).isEqualTo(latitude);
        assertThat(metadata.getLongitude()).isEqualTo(longitude);
        assertThat(metadata.getDistance()).isEqualTo(distance);
        assertThat(metadata.getPrecipitationMin()).isEqualTo(precipMin);
        assertThat(metadata.getPrecipitationMax()).isEqualTo(precipMax);
        assertThat(metadata.getPrecipitationAvg()).isEqualTo(precipAvg);
        assertThat(metadata.getCoveragePercent()).isEqualTo(coveragePercent);
        assertThat(metadata.getSignificantRainCells()).isEqualTo(significantRainCells);
        assertThat(metadata.getTotalCells()).isEqualTo(totalCells);
        assertThat(metadata.getGeometryJson()).isEqualTo(geometryJson);
        assertThat(metadata.getBboxPixels()).isEqualTo(bboxPixels);
        assertThat(metadata.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("@PrePersist onCreate should set createdAt when null")
    void prePersist_onCreate_setsCreatedAtWhenNull() throws Exception {
        // Arrange
        WeatherRadarMetadata metadata = new WeatherRadarMetadata();
        metadata.setCreatedAt(null);

        // Get the @PrePersist method via reflection
        Method onCreateMethod = WeatherRadarMetadata.class.getDeclaredMethod("onCreate");
        onCreateMethod.setAccessible(true);

        // Act
        onCreateMethod.invoke(metadata);

        // Assert
        assertThat(metadata.getCreatedAt()).isNotNull();
        assertThat(metadata.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("@PrePersist onCreate should not overwrite existing createdAt")
    void prePersist_onCreate_doesNotOverwriteExistingCreatedAt() throws Exception {
        // Arrange
        LocalDateTime existingCreatedAt = LocalDateTime.of(2024, 1, 1, 10, 0);
        WeatherRadarMetadata metadata = new WeatherRadarMetadata();
        metadata.setCreatedAt(existingCreatedAt);

        // Get the @PrePersist method via reflection
        Method onCreateMethod = WeatherRadarMetadata.class.getDeclaredMethod("onCreate");
        onCreateMethod.setAccessible(true);

        // Act
        onCreateMethod.invoke(metadata);

        // Assert
        assertThat(metadata.getCreatedAt()).isEqualTo(existingCreatedAt);
    }

    @Test
    @DisplayName("WeatherRadarMetadata should allow setting and getting all fields")
    void weatherRadarMetadata_settersAndGetters_workCorrectly() {
        // Arrange
        WeatherRadarMetadata metadata = new WeatherRadarMetadata();
        LocalDateTime timestamp = LocalDateTime.of(2025, 1, 15, 14, 30);
        LocalDateTime createdAt = LocalDateTime.now();

        // Act
        metadata.setId(42L);
        metadata.setDeviceId("dev-789");
        metadata.setTimestamp(timestamp);
        metadata.setSource("DWD_RV");
        metadata.setLatitude(51.5F);
        metadata.setLongitude(-0.1F);
        metadata.setDistance(10000);
        metadata.setPrecipitationMin(0.1F);
        metadata.setPrecipitationMax(15.8F);
        metadata.setPrecipitationAvg(4.2F);
        metadata.setCoveragePercent(62.5F);
        metadata.setSignificantRainCells(200);
        metadata.setTotalCells(800);
        metadata.setGeometryJson("{\"type\":\"Polygon\"}");
        metadata.setBboxPixels("50,100,150,200");
        metadata.setCreatedAt(createdAt);

        // Assert
        assertThat(metadata.getId()).isEqualTo(42L);
        assertThat(metadata.getDeviceId()).isEqualTo("dev-789");
        assertThat(metadata.getTimestamp()).isEqualTo(timestamp);
        assertThat(metadata.getSource()).isEqualTo("DWD_RV");
        assertThat(metadata.getLatitude()).isEqualTo(51.5F);
        assertThat(metadata.getLongitude()).isEqualTo(-0.1F);
        assertThat(metadata.getDistance()).isEqualTo(10000);
        assertThat(metadata.getPrecipitationMin()).isEqualTo(0.1F);
        assertThat(metadata.getPrecipitationMax()).isEqualTo(15.8F);
        assertThat(metadata.getPrecipitationAvg()).isEqualTo(4.2F);
        assertThat(metadata.getCoveragePercent()).isEqualTo(62.5F);
        assertThat(metadata.getSignificantRainCells()).isEqualTo(200);
        assertThat(metadata.getTotalCells()).isEqualTo(800);
        assertThat(metadata.getGeometryJson()).isEqualTo("{\"type\":\"Polygon\"}");
        assertThat(metadata.getBboxPixels()).isEqualTo("50,100,150,200");
        assertThat(metadata.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("WeatherRadarMetadata should handle null optional fields")
    void weatherRadarMetadata_handlesNullOptionalFields() {
        // Arrange
        WeatherRadarMetadata metadata = new WeatherRadarMetadata();

        // Act
        metadata.setDeviceId(null);
        metadata.setSource(null);
        metadata.setPrecipitationMin(null);
        metadata.setPrecipitationMax(null);
        metadata.setPrecipitationAvg(null);
        metadata.setCoveragePercent(null);
        metadata.setSignificantRainCells(null);
        metadata.setTotalCells(null);
        metadata.setGeometryJson(null);
        metadata.setBboxPixels(null);

        // Assert
        assertThat(metadata.getDeviceId()).isNull();
        assertThat(metadata.getSource()).isNull();
        assertThat(metadata.getPrecipitationMin()).isNull();
        assertThat(metadata.getPrecipitationMax()).isNull();
        assertThat(metadata.getPrecipitationAvg()).isNull();
        assertThat(metadata.getCoveragePercent()).isNull();
        assertThat(metadata.getSignificantRainCells()).isNull();
        assertThat(metadata.getTotalCells()).isNull();
        assertThat(metadata.getGeometryJson()).isNull();
        assertThat(metadata.getBboxPixels()).isNull();
    }

    @Test
    @DisplayName("WeatherRadarMetadata should handle zero precipitation values")
    void weatherRadarMetadata_handlesZeroPrecipitation() {
        // Arrange
        WeatherRadarMetadata metadata = new WeatherRadarMetadata();

        // Act
        metadata.setPrecipitationMin(0.0F);
        metadata.setPrecipitationMax(0.0F);
        metadata.setPrecipitationAvg(0.0F);
        metadata.setCoveragePercent(0.0F);
        metadata.setSignificantRainCells(0);

        // Assert
        assertThat(metadata.getPrecipitationMin()).isEqualTo(0.0F);
        assertThat(metadata.getPrecipitationMax()).isEqualTo(0.0F);
        assertThat(metadata.getPrecipitationAvg()).isEqualTo(0.0F);
        assertThat(metadata.getCoveragePercent()).isEqualTo(0.0F);
        assertThat(metadata.getSignificantRainCells()).isEqualTo(0);
    }

    @Test
    @DisplayName("WeatherRadarMetadata should handle heavy precipitation values")
    void weatherRadarMetadata_handlesHeavyPrecipitation() {
        // Arrange
        WeatherRadarMetadata metadata = new WeatherRadarMetadata();

        // Act
        metadata.setPrecipitationMin(5.0F);
        metadata.setPrecipitationMax(50.0F);
        metadata.setPrecipitationAvg(20.5F);
        metadata.setCoveragePercent(100.0F);
        metadata.setSignificantRainCells(500);
        metadata.setTotalCells(500);

        // Assert
        assertThat(metadata.getPrecipitationMax()).isEqualTo(50.0F);
        assertThat(metadata.getCoveragePercent()).isEqualTo(100.0F);
        assertThat(metadata.getSignificantRainCells()).isEqualTo(500);
    }

    @Test
    @DisplayName("WeatherRadarMetadata equality should work with Lombok @Data")
    void weatherRadarMetadata_equality_worksWithLombok() {
        // Arrange
        LocalDateTime timestamp = LocalDateTime.of(2025, 1, 1, 12, 0);
        WeatherRadarMetadata metadata1 = new WeatherRadarMetadata();
        metadata1.setId(1L);
        metadata1.setDeviceId("device-123");
        metadata1.setTimestamp(timestamp);
        metadata1.setLatitude(52.52F);
        metadata1.setLongitude(13.41F);
        metadata1.setDistance(5000);

        WeatherRadarMetadata metadata2 = new WeatherRadarMetadata();
        metadata2.setId(1L);
        metadata2.setDeviceId("device-123");
        metadata2.setTimestamp(timestamp);
        metadata2.setLatitude(52.52F);
        metadata2.setLongitude(13.41F);
        metadata2.setDistance(5000);

        WeatherRadarMetadata metadata3 = new WeatherRadarMetadata();
        metadata3.setId(2L);
        metadata3.setDeviceId("device-456");
        metadata3.setTimestamp(timestamp.plusHours(1));
        metadata3.setLatitude(51.5F);
        metadata3.setLongitude(-0.1F);
        metadata3.setDistance(10000);

        // Assert
        assertThat(metadata1).isEqualTo(metadata2);
        assertThat(metadata1).isNotEqualTo(metadata3);
        assertThat(metadata1.hashCode()).isEqualTo(metadata2.hashCode());
    }

    @Test
    @DisplayName("WeatherRadarMetadata should handle complex GeoJSON")
    void weatherRadarMetadata_handlesComplexGeoJson() {
        // Arrange
        WeatherRadarMetadata metadata = new WeatherRadarMetadata();
        String complexGeoJson = """
                {
                  "type": "Polygon",
                  "coordinates": [
                    [
                      [13.0, 52.0],
                      [14.0, 52.0],
                      [14.0, 53.0],
                      [13.0, 53.0],
                      [13.0, 52.0]
                    ]
                  ]
                }
                """;

        // Act
        metadata.setGeometryJson(complexGeoJson);

        // Assert
        assertThat(metadata.getGeometryJson()).isEqualTo(complexGeoJson);
        assertThat(metadata.getGeometryJson()).contains("\"type\": \"Polygon\"");
        assertThat(metadata.getGeometryJson()).contains("coordinates");
    }
}
