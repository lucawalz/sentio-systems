package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.dto.AnimalDetectionSummary;
import org.example.backend.model.AnimalDetection;
import org.example.backend.repository.AnimalDetectionRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Query service for animal detection read operations (CQRS pattern).
 * Handles: retrieval, filtering, aggregation, and analytics queries.
 * Separated from command operations for better scalability and maintainability.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnimalDetectionQueryService {

    private final AnimalDetectionRepository animalDetectionRepository;
    private final DeviceService deviceService;

    /**
     * Retrieves the most recent animal detections up to the specified limit.
     */
    public List<AnimalDetection> getLatestDetections(int limit) {
        log.debug("Retrieving latest {} animal detections", limit);
        List<String> deviceIds = deviceService.getMyDeviceIds();
        if (deviceIds.isEmpty()) {
            return Collections.emptyList();
        }
        return animalDetectionRepository.findTopNByDeviceIdInOrderByTimestampDesc(deviceIds, limit);
    }

    /**
     * Retrieves animal detections from the specified number of hours ago.
     */
    public List<AnimalDetection> getRecentDetections(int hours) {
        log.debug("Retrieving animal detections from the last {} hours", hours);
        List<String> deviceIds = deviceService.getMyDeviceIds();
        if (deviceIds.isEmpty()) {
            return Collections.emptyList();
        }
        LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
        return animalDetectionRepository.findRecentDetectionsByDeviceIds(deviceIds, startTime);
    }

    /**
     * Retrieves all animal detections for a specific date.
     */
    public List<AnimalDetection> getDetectionsByDate(LocalDate date) {
        log.debug("Retrieving animal detections for date: {}", date);
        List<String> deviceIds = deviceService.getMyDeviceIds();
        if (deviceIds.isEmpty()) {
            return Collections.emptyList();
        }
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        return animalDetectionRepository.findByDeviceIdInAndTimestampBetweenOrderByTimestampDesc(
                deviceIds, startOfDay, endOfDay);
    }

    /**
     * Retrieves animal detections for a specific species with pagination.
     */
    public List<AnimalDetection> getDetectionsBySpecies(String species, Pageable pageable) {
        log.debug("Retrieving detections for species: {} with pagination", species);
        List<String> deviceIds = deviceService.getMyDeviceIds();
        if (deviceIds.isEmpty()) {
            return Collections.emptyList();
        }
        return animalDetectionRepository.findByDeviceIdInAndSpeciesIgnoreCaseOrderByTimestampDesc(
                deviceIds, species, pageable);
    }

    /**
     * Retrieves animal detections for a specific animal type with pagination.
     */
    public List<AnimalDetection> getDetectionsByAnimalType(String animalType, Pageable pageable) {
        log.debug("Retrieving detections for animal type: {} with pagination", animalType);
        List<String> deviceIds = deviceService.getMyDeviceIds();
        if (deviceIds.isEmpty()) {
            return Collections.emptyList();
        }
        return animalDetectionRepository.findByDeviceIdInAndAnimalTypeIgnoreCaseOrderByTimestampDesc(
                deviceIds, animalType, pageable);
    }

    /**
     * Retrieves animal detections from a specific device with pagination.
     */
    public List<AnimalDetection> getDetectionsByDevice(String deviceId, Pageable pageable) {
        log.debug("Retrieving detections for device: {} with pagination", deviceId);
        if (!deviceService.hasAccessToDevice(deviceId)) {
            log.warn("User attempted to access detections for device {} without ownership", deviceId);
            throw new org.springframework.security.access.AccessDeniedException("Access denied to device");
        }
        return animalDetectionRepository.findByDeviceIdOrderByTimestampDesc(deviceId, pageable);
    }

    /**
     * Retrieves high-confidence animal detections within a specified timeframe.
     */
    public List<AnimalDetection> getHighConfidenceDetections(float minConfidence, int hours) {
        log.debug("Retrieving high-confidence detections (>= {}) from last {} hours", minConfidence, hours);
        LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
        return animalDetectionRepository.findHighConfidenceDetections(minConfidence, startTime);
    }

    /**
     * Retrieves an animal detection by its unique identifier.
     */
    public Optional<AnimalDetection> getDetectionById(Long id) {
        log.debug("Retrieving animal detection by ID: {}", id);
        return animalDetectionRepository.findById(id)
                .filter(d -> deviceService.hasAccessToDevice(d.getDeviceId()));
    }

    /**
     * Generates comprehensive detection summary with statistics.
     */
    public AnimalDetectionSummary getDetectionSummary(int hours) {
        log.debug("Generating detection summary for last {} hours", hours);
        LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
        List<AnimalDetection> recentDetections = animalDetectionRepository.findRecentDetections(startTime);

        AnimalDetectionSummary summary = new AnimalDetectionSummary();
        summary.setTotalDetections(recentDetections.size());

        if (!recentDetections.isEmpty()) {
            populateSummaryStats(summary, recentDetections);
        }

        return summary;
    }

    /**
     * Populates the detection summary with statistics from a list of detections.
     *
     * Calculates unique species, unique animal types, average confidence,
     * first/last detection timestamps, species and animal type breakdowns,
     * and counts for birds, mammals, and other animals.
     *
     * @param summary    The summary object to populate
     * @param detections The list of animal detections to analyze
     */
    private void populateSummaryStats(AnimalDetectionSummary summary, List<AnimalDetection> detections) {
        Set<String> uniqueSpecies = detections.stream()
                .map(AnimalDetection::getSpecies)
                .collect(Collectors.toSet());
        summary.setUniqueSpecies(uniqueSpecies.size());

        Set<String> uniqueAnimalTypes = detections.stream()
                .map(AnimalDetection::getAnimalType)
                .collect(Collectors.toSet());
        summary.setUniqueAnimalTypes(uniqueAnimalTypes.size());

        double avgConfidence = detections.stream()
                .mapToDouble(AnimalDetection::getConfidence)
                .average()
                .orElse(0.0);
        summary.setAverageConfidence(avgConfidence);

        summary.setFirstDetection(detections.stream()
                .min(Comparator.comparing(AnimalDetection::getTimestamp))
                .map(AnimalDetection::getTimestamp)
                .orElse(null));

        summary.setLastDetection(detections.stream()
                .max(Comparator.comparing(AnimalDetection::getTimestamp))
                .map(AnimalDetection::getTimestamp)
                .orElse(null));

        Map<String, Long> speciesBreakdown = detections.stream()
                .collect(Collectors.groupingBy(AnimalDetection::getSpecies, Collectors.counting()));
        summary.setSpeciesBreakdown(speciesBreakdown);

        Map<String, Long> animalTypeBreakdown = detections.stream()
                .collect(Collectors.groupingBy(AnimalDetection::getAnimalType, Collectors.counting()));
        summary.setAnimalTypeBreakdown(animalTypeBreakdown);

        long birdCount = detections.stream().filter(AnimalDetection::isBird).count();
        summary.setBirdDetections(birdCount);

        long mammalCount = detections.stream().filter(AnimalDetection::isMammal).count();
        summary.setMammalDetections(mammalCount);

        summary.setOtherAnimalDetections(detections.size() - birdCount - mammalCount);
    }

    /**
     * Generates species count statistics for a given timeframe.
     */
    public Map<String, Long> getSpeciesCount(int hours) {
        log.debug("Generating species count for last {} hours", hours);
        LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
        List<AnimalDetection> recentDetections = animalDetectionRepository.findRecentDetections(startTime);

        return recentDetections.stream()
                .collect(Collectors.groupingBy(AnimalDetection::getSpecies, Collectors.counting()));
    }

    /**
     * Generates animal type count statistics for a given timeframe.
     */
    public Map<String, Long> getAnimalTypeCount(int hours) {
        log.debug("Generating animal type count for last {} hours", hours);
        LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
        List<AnimalDetection> recentDetections = animalDetectionRepository.findRecentDetections(startTime);

        return recentDetections.stream()
                .collect(Collectors.groupingBy(AnimalDetection::getAnimalType, Collectors.counting()));
    }

    /**
     * Generates hourly activity distribution for a specific date.
     */
    public Map<Integer, Long> getHourlyActivity(LocalDate date) {
        log.debug("Generating hourly activity for date: {}", date);
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        List<AnimalDetection> dayDetections = animalDetectionRepository
                .findByTimestampBetweenOrderByTimestampDesc(startOfDay, endOfDay);

        return dayDetections.stream()
                .collect(Collectors.groupingBy(d -> d.getTimestamp().getHour(), Collectors.counting()));
    }

    /**
     * Retrieves all distinct animal species from the database.
     */
    public List<String> getAllSpecies() {
        log.debug("Retrieving all distinct species");
        return animalDetectionRepository.findAllDistinctSpecies();
    }

    /**
     * Retrieves all distinct animal types from the database.
     */
    public List<String> getAllAnimalTypes() {
        log.debug("Retrieving all distinct animal types");
        return animalDetectionRepository.findAllDistinctAnimalTypes();
    }

    /**
     * Retrieves bird detections specifically (backward compatibility).
     */
    public List<AnimalDetection> getBirdDetections(int hours) {
        log.debug("Retrieving bird detections from the last {} hours", hours);
        LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
        return animalDetectionRepository.findBirdDetections(startTime);
    }

    /**
     * Retrieves mammal detections specifically.
     */
    public List<AnimalDetection> getMammalDetections(int hours) {
        log.debug("Retrieving mammal detections from the last {} hours", hours);
        LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
        return animalDetectionRepository.findMammalDetections(startTime);
    }

    /**
     * Generates comprehensive system-wide statistics.
     */
    public Map<String, Object> getSystemStats() {
        log.debug("Generating comprehensive system statistics");
        Map<String, Object> stats = new HashMap<>();

        long totalDetections = animalDetectionRepository.count();
        stats.put("totalDetections", totalDetections);

        List<AnimalDetection> allDetections = animalDetectionRepository.findAll();

        Map<String, Long> allSpeciesCount = allDetections.stream()
                .collect(Collectors.groupingBy(AnimalDetection::getSpecies, Collectors.counting()));
        stats.put("uniqueSpeciesCount", allSpeciesCount.size());

        allSpeciesCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .ifPresent(entry -> {
                    stats.put("mostDetectedSpecies", entry.getKey());
                    stats.put("mostDetectedSpeciesCount", entry.getValue());
                });

        double avgConfidence = allDetections.stream()
                .mapToDouble(AnimalDetection::getConfidence)
                .average()
                .orElse(0.0);
        stats.put("averageConfidence", Math.round(avgConfidence * 100.0) / 100.0);

        long birdDetections = allDetections.stream().filter(AnimalDetection::isBird).count();
        stats.put("birdDetections", birdDetections);

        long mammalDetections = allDetections.stream().filter(AnimalDetection::isMammal).count();
        stats.put("mammalDetections", mammalDetections);

        return stats;
    }
}
