package org.example.backend.repository;

import org.example.backend.model.AnimalDetection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Animal DetectionRepository.
 * Tests all custom query methods and database constraints.
 */
class AnimalDetectionRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private AnimalDetectionRepository repository;

    private LocalDateTime now;
    private LocalDateTime oneHourAgo;
    private LocalDateTime oneDayAgo;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        now = LocalDateTime.now();
        oneHourAgo = now.minusHours(1);
        oneDayAgo = now.minusDays(1);
    }

    @Test
    void testSaveAndFindById() {
        printTestHeader("Save and Find By ID");

        AnimalDetection detection = createDetection("Sparrow", "bird", 0.95f, "device-1");
        AnimalDetection saved = repository.save(detection);

        assertThat(saved.getId()).isNotNull();
        assertThat(repository.findById(saved.getId())).isPresent();
    }

    @Test
    void testFindRecentDetections() {
        printTestHeader("Find Recent Detections");

        // Create detections at different times
        repository.save(createDetectionWithTimestamp("Sparrow", "bird", 0.9f, oneDayAgo, "device-1"));
        repository.save(createDetectionWithTimestamp("Robin", "bird", 0.85f, oneHourAgo, "device-1"));
        repository.save(createDetectionWithTimestamp("Eagle", "bird", 0.92f, now, "device-1"));

        List<AnimalDetection> recent = repository.findRecentDetections(oneHourAgo);

        assertThat(recent).hasSize(2);
        assertThat(recent.get(0).getSpecies()).isEqualTo("Eagle"); // Most recent first
    }

    @Test
    void testFindTopNByOrderByTimestampDesc() {
        printTestHeader("Find Top N By Timestamp");

        for (int i = 0; i < 15; i++) {
            repository.save(createDetection("Bird" + i, "bird", 0.9f, "device-1"));
        }

        List<AnimalDetection> top10 = repository.findTopNByOrderByTimestampDesc(10);

        assertThat(top10).hasSize(10);
    }

    @Test
    void testFindTop10ByOrderByTimestampDesc() {
        printTestHeader("Find Top 10");

        for (int i = 0; i < 15; i++) {
            repository.save(createDetection("Bird" + i, "bird", 0.9f, "device-1"));
        }

        List<AnimalDetection> top10 = repository.findTop10ByOrderByTimestampDesc();

        assertThat(top10).hasSize(10);
    }

    @Test
    void testFindByTimestampBetween() {
        printTestHeader("Find By Timestamp Between");

        repository.save(createDetectionWithTimestamp("Old", "bird", 0.9f, oneDayAgo.minusDays(1), "device-1"));
        repository.save(createDetectionWithTimestamp("Recent", "bird", 0.85f, oneHourAgo, "device-1"));
        repository.save(createDetectionWithTimestamp("New", "bird", 0.92f, now, "device-1"));

        List<AnimalDetection> inRange = repository.findByTimestampBetweenOrderByTimestampDesc(oneDayAgo, now);

        assertThat(inRange).hasSize(2);
    }

    @Test
    void testFindByDeviceIdInAndTimestampBetween() {
        printTestHeader("Find By Device IDs and Timestamp");

        repository.save(createDetectionWithTimestamp("Bird1", "bird", 0.9f, oneHourAgo, "device-1"));
        repository.save(createDetectionWithTimestamp("Bird2", "bird", 0.85f, oneHourAgo, "device-2"));
        repository.save(createDetectionWithTimestamp("Bird3", "bird", 0.92f, oneHourAgo, "device-3"));

        List<AnimalDetection> results = repository.findByDeviceIdInAndTimestampBetweenOrderByTimestampDesc(
                Arrays.asList("device-1", "device-2"), oneDayAgo, now);

        assertThat(results).hasSize(2);
    }

    @Test
    void testFindBySpeciesIgnoreCase() {
        printTestHeader("Find By Species (Case Insensitive)");

        repository.save(createDetection("sparrow", "bird", 0.9f, "device-1"));
        repository.save(createDetection("SPARROW", "bird", 0.85f, "device-1"));
        repository.save(createDetection("Robin", "bird", 0.92f, "device-1"));

        List<AnimalDetection> sparrows = repository.findBySpeciesIgnoreCaseOrderByTimestampDesc(
                "Sparrow", PageRequest.of(0, 10));

        assertThat(sparrows).hasSize(2);
    }

    @Test
    void testFindByAnimalTypeIgnoreCase() {
        printTestHeader("Find By Animal Type");

        repository.save(createDetection("Sparrow", "bird", 0.9f, "device-1"));
        repository.save(createDetection("Robin", "bird", 0.85f, "device-1"));
        repository.save(createDetection("Deer", "mammal", 0.92f, "device-1"));

        List<AnimalDetection> birds = repository.findByAnimalTypeIgnoreCaseOrderByTimestampDesc(
                "bird", PageRequest.of(0, 10));

        assertThat(birds).hasSize(2);
    }

    @Test
    void testFindHighConfidenceDetections() {
        printTestHeader("Find High Confidence Detections");

        repository.save(createDetection("Sparrow", "bird", 0.95f, "device-1"));
        repository.save(createDetection("Robin", "bird", 0.75f, "device-1"));
        repository.save(createDetection("Eagle", "bird", 0.92f, "device-1"));

        List<AnimalDetection> highConf = repository.findHighConfidenceDetections(0.9f, oneDayAgo);

        assertThat(highConf).hasSize(2);
        assertThat(highConf).allMatch(d -> d.getConfidence() >= 0.9f);
    }

    @Test
    void testCountUniqueSpeciesSince() {
        printTestHeader("Count Unique Species");

        repository.save(createDetectionWithTimestamp("Sparrow", "bird", 0.9f, oneHourAgo, "device-1"));
        repository.save(createDetectionWithTimestamp("Sparrow", "bird", 0.85f, oneHourAgo, "device-1"));
        repository.save(createDetectionWithTimestamp("Robin", "bird", 0.92f, oneHourAgo, "device-1"));
        repository.save(createDetectionWithTimestamp("Eagle", "bird", 0.88f, oneHourAgo, "device-1"));

        long count = repository.countUniqueSpeciesSince(oneDayAgo);

        assertThat(count).isEqualTo(3); // Sparrow, Robin, Eagle
    }

    @Test
    void testCountUniqueAnimalTypesSince() {
        printTestHeader("Count Unique Animal Types");

        repository.save(createDetectionWithTimestamp("Sparrow", "bird", 0.9f, oneHourAgo, "device-1"));
        repository.save(createDetectionWithTimestamp("Robin", "bird", 0.85f, oneHourAgo, "device-1"));
        repository.save(createDetectionWithTimestamp("Deer", "mammal", 0.92f, oneHourAgo, "device-1"));
        repository.save(createDetectionWithTimestamp("Fox", "mammal", 0.88f, oneHourAgo, "device-1"));

        long count = repository.countUniqueAnimalTypesSince(oneDayAgo);

        assertThat(count).isEqualTo(2); // bird, mammal
    }

    @Test
    void testCountActiveDevices() {
        printTestHeader("Count Active Devices");

        repository.save(createDetection("Sparrow", "bird", 0.9f, "device-1"));
        repository.save(createDetection("Robin", "bird", 0.85f, "device-1"));
        repository.save(createDetection("Eagle", "bird", 0.92f, "device-2"));
        repository.save(createDetection("Deer", "mammal", 0.88f, "device-3"));

        long count = repository.countActiveDevices();

        assertThat(count).isEqualTo(3);
    }

    @Test
    void testFindAverageConfidenceSince() {
        printTestHeader("Find Average Confidence");

        repository.save(createDetectionWithTimestamp("Sparrow", "bird", 1.0f, oneHourAgo, "device-1"));
        repository.save(createDetectionWithTimestamp("Robin", "bird", 0.8f, oneHourAgo, "device-1"));

        Double avgConfidence = repository.findAverageConfidenceSince(oneDayAgo);

        assertThat(avgConfidence).isCloseTo(0.9, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void testFindAllDistinctSpecies() {
        printTestHeader("Find All Distinct Species");

        repository.save(createDetection("Sparrow", "bird", 0.9f, "device-1"));
        repository.save(createDetection("Sparrow", "bird", 0.85f, "device-1"));
        repository.save(createDetection("Robin", "bird", 0.92f, "device-1"));
        repository.save(createDetection("Deer", "mammal", 0.88f, "device-1"));

        List<String> species = repository.findAllDistinctSpecies();

        assertThat(species).containsExactlyInAnyOrder("Deer", "Robin", "Sparrow");
    }

    @Test
    void testBirdSpecificQueries() {
        printTestHeader("Bird-Specific Queries");

        repository.save(createDetectionWithTimestamp("Sparrow", "bird", 0.9f, oneHourAgo, "device-1"));
        repository.save(createDetectionWithTimestamp("Robin", "bird", 0.85f, oneHourAgo, "device-1"));
        repository.save(createDetectionWithTimestamp("Deer", "mammal", 0.92f, oneHourAgo, "device-1"));

        List<AnimalDetection> birdDetections = repository.findBirdDetections(oneDayAgo);
        long birdCount = repository.countBirdDetectionsSince(oneDayAgo);

        assertThat(birdDetections).hasSize(2);
        assertThat(birdCount).isEqualTo(2);
        assertThat(birdDetections).allMatch(d -> d.getAnimalType().equals("bird"));
    }

    @Test
    void testMammalSpecificQueries() {
        printTestHeader("Mammal-Specific Queries");

        repository.save(createDetectionWithTimestamp("Sparrow", "bird", 0.9f, oneHourAgo, "device-1"));
        repository.save(createDetectionWithTimestamp("Deer", "mammal", 0.85f, oneHourAgo, "device-1"));
        repository.save(createDetectionWithTimestamp("Fox", "mammal", 0.92f, oneHourAgo, "device-1"));

        List<AnimalDetection> mammalDetections = repository.findMammalDetections(oneDayAgo);
        long mammalCount = repository.countMammalDetectionsSince(oneDayAgo);

        assertThat(mammalDetections).hasSize(2);
        assertThat(mammalCount).isEqualTo(2);
        assertThat(mammalDetections).allMatch(d -> d.getAnimalType().equals("mammal"));
    }

    // Helper methods

    private AnimalDetection createDetection(String species, String animalType, float confidence, String deviceId) {
        AnimalDetection detection = new AnimalDetection();
        detection.setSpecies(species);
        detection.setAnimalType(animalType);
        detection.setConfidence(confidence);
        detection.setX(100f);
        detection.setY(100f);
        detection.setWidth(50f);
        detection.setHeight(50f);
        detection.setTimestamp(now);
        detection.setDeviceId(deviceId);
        return detection;
    }

    private AnimalDetection createDetectionWithTimestamp(String species, String animalType,
            float confidence, LocalDateTime timestamp,
            String deviceId) {
        AnimalDetection detection = createDetection(species, animalType, confidence, deviceId);
        detection.setTimestamp(timestamp);
        return detection;
    }
}
