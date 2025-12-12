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
 * Service responsible for managing animal detection data and analytics
 * operations.
 * Handles persistence, retrieval, and statistical analysis of AI-powered animal
 * detection results.
 * <p>
 * This service provides comprehensive functionality for storing detection data
 * from
 * computer vision models, generating analytics summaries, and managing
 * associated
 * image files. It supports various filtering and aggregation operations for
 * detailed animal activity analysis across multiple species types.
 * </p>
 *
 * @author Sentio Team
 * @version 2.0
 * @since 2.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnimalDetectionService {

        private final AnimalDetectionRepository animalDetectionRepository;
        private final ImageStorageService imageStorageService;
        private final DeviceService deviceService;

        /**
         * Persists a new animal detection record to the database.
         *
         * @param animalDetection The animal detection data to save
         * @return The saved animal detection with generated ID and timestamps
         */
        public AnimalDetection saveAnimalDetection(AnimalDetection animalDetection) {
                log.info("Saving animal detection - Type: {}, Species: {} with confidence: {:.2f}",
                                animalDetection.getAnimalType(), animalDetection.getSpecies(),
                                animalDetection.getConfidence());
                AnimalDetection saved = animalDetectionRepository.save(animalDetection);
                log.debug("Successfully saved animal detection with ID: {}", saved.getId());
                return saved;
        }

        /**
         * Retrieves the most recent animal detections up to the specified limit.
         *
         * @param limit Maximum number of detections to retrieve
         * @return List of latest animal detections ordered by timestamp (newest first)
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
         *
         * @param hours Number of hours to look back from current time
         * @return List of animal detections within the timeframe
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
         *
         * @param date The target date for detection retrieval
         * @return List of animal detections for the specified date
         */
        public List<AnimalDetection> getDetectionsByDate(LocalDate date) {
                log.debug("Retrieving animal detections for date: {}", date);
                List<String> deviceIds = deviceService.getMyDeviceIds();
                if (deviceIds.isEmpty()) {
                        return Collections.emptyList();
                }
                LocalDateTime startOfDay = date.atStartOfDay();
                LocalDateTime endOfDay = date.atTime(23, 59, 59);
                return animalDetectionRepository.findByDeviceIdInAndTimestampBetweenOrderByTimestampDesc(deviceIds,
                                startOfDay, endOfDay);
        }

        /**
         * Retrieves animal detections for a specific species with pagination support.
         *
         * @param species  The animal species name to filter by
         * @param pageable Pagination configuration
         * @return List of animal detections for the specified species
         */
        public List<AnimalDetection> getDetectionsBySpecies(String species, Pageable pageable) {
                log.debug("Retrieving detections for species: {} with pagination", species);
                List<String> deviceIds = deviceService.getMyDeviceIds();
                if (deviceIds.isEmpty()) {
                        return Collections.emptyList();
                }
                return animalDetectionRepository.findByDeviceIdInAndSpeciesIgnoreCaseOrderByTimestampDesc(deviceIds,
                                species, pageable);
        }

        /**
         * Retrieves animal detections for a specific animal type with pagination
         * support.
         *
         * @param animalType The animal type to filter by (e.g., "bird", "mammal")
         * @param pageable   Pagination configuration
         * @return List of animal detections for the specified type
         */
        public List<AnimalDetection> getDetectionsByAnimalType(String animalType, Pageable pageable) {
                log.debug("Retrieving detections for animal type: {} with pagination", animalType);
                List<String> deviceIds = deviceService.getMyDeviceIds();
                if (deviceIds.isEmpty()) {
                        return Collections.emptyList();
                }
                return animalDetectionRepository.findByDeviceIdInAndAnimalTypeIgnoreCaseOrderByTimestampDesc(deviceIds,
                                animalType, pageable);
        }

        /**
         * Retrieves animal detections from a specific device with pagination support.
         *
         * @param deviceId The device identifier to filter by
         * @param pageable Pagination configuration
         * @return List of animal detections from the specified device
         */
        public List<AnimalDetection> getDetectionsByDevice(String deviceId, Pageable pageable) {
                log.debug("Retrieving detections for device: {} with pagination", deviceId);
                // Verify ownership
                if (!deviceService.hasAccessToDevice(deviceId)) {
                        log.warn("User attempted to access detections for device {} without ownership", deviceId);
                        throw new org.springframework.security.access.AccessDeniedException("Access denied to device");
                }
                return animalDetectionRepository.findByDeviceIdOrderByTimestampDesc(deviceId, pageable);
        }

        /**
         * Retrieves high-confidence animal detections within a specified timeframe.
         *
         * @param minConfidence Minimum confidence threshold (0.0 to 1.0)
         * @param hours         Number of hours to look back
         * @return List of high-confidence animal detections
         */
        public List<AnimalDetection> getHighConfidenceDetections(float minConfidence, int hours) {
                log.debug("Retrieving high-confidence detections (>= {}) from last {} hours", minConfidence, hours);
                LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
                return animalDetectionRepository.findHighConfidenceDetections(minConfidence, startTime);
        }

        /**
         * Retrieves an animal detection by its unique identifier.
         *
         * @param id The unique detection ID
         * @return Optional containing the detection if found, empty otherwise
         */
        public Optional<AnimalDetection> getDetectionById(Long id) {
                log.debug("Retrieving animal detection by ID: {}", id);
                return animalDetectionRepository.findById(id)
                                .filter(d -> deviceService.hasAccessToDevice(d.getDeviceId()));
        }

        /**
         * Deletes an animal detection and its associated image file.
         *
         * @param id The unique detection ID to delete
         * @return true if deletion was successful, false if detection not found
         */
        public boolean deleteDetection(Long id) {
                log.debug("Attempting to delete animal detection with ID: {}", id);
                Optional<AnimalDetection> detection = animalDetectionRepository.findById(id);
                if (detection.isPresent()) {
                        if (!deviceService.hasAccessToDevice(detection.get().getDeviceId())) {
                                log.warn("User attempted to delete detection {} without ownership of device", id);
                                throw new org.springframework.security.access.AccessDeniedException(
                                                "Access denied to delete detection");
                        }
                        // Delete associated image if it exists
                        if (detection.get().getImageUrl() != null) {
                                boolean imageDeleted = imageStorageService.deleteImage(detection.get().getImageUrl());
                                log.debug("Associated image deletion result: {}", imageDeleted);
                        }
                        animalDetectionRepository.deleteById(id);
                        log.info("Successfully deleted animal detection with ID: {}", id);
                        return true;
                }
                log.warn("Animal detection with ID {} not found for deletion", id);
                return false;
        }

        /**
         * Generates comprehensive detection summary with statistics for a given
         * timeframe.
         *
         * @param hours Number of hours to analyze
         * @return AnimalDetectionSummary containing various metrics and breakdowns
         */
        public AnimalDetectionSummary getDetectionSummary(int hours) {
                log.debug("Generating detection summary for last {} hours", hours);
                LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
                List<AnimalDetection> recentDetections = animalDetectionRepository.findRecentDetections(startTime);

                AnimalDetectionSummary summary = new AnimalDetectionSummary();
                summary.setTotalDetections(recentDetections.size());

                if (!recentDetections.isEmpty()) {
                        // Calculate unique species count
                        Set<String> uniqueSpecies = recentDetections.stream()
                                        .map(AnimalDetection::getSpecies)
                                        .collect(Collectors.toSet());
                        summary.setUniqueSpecies(uniqueSpecies.size());

                        // Calculate unique animal types count
                        Set<String> uniqueAnimalTypes = recentDetections.stream()
                                        .map(AnimalDetection::getAnimalType)
                                        .collect(Collectors.toSet());
                        summary.setUniqueAnimalTypes(uniqueAnimalTypes.size());

                        // Calculate average confidence
                        double avgConfidence = recentDetections.stream()
                                        .mapToDouble(AnimalDetection::getConfidence)
                                        .average()
                                        .orElse(0.0);
                        summary.setAverageConfidence(avgConfidence);

                        // Find first and last detection times
                        summary.setFirstDetection(recentDetections.stream()
                                        .min(Comparator.comparing(AnimalDetection::getTimestamp))
                                        .map(AnimalDetection::getTimestamp)
                                        .orElse(null));

                        summary.setLastDetection(recentDetections.stream()
                                        .max(Comparator.comparing(AnimalDetection::getTimestamp))
                                        .map(AnimalDetection::getTimestamp)
                                        .orElse(null));

                        // Generate species breakdown
                        Map<String, Long> speciesBreakdown = recentDetections.stream()
                                        .collect(Collectors.groupingBy(
                                                        AnimalDetection::getSpecies,
                                                        Collectors.counting()));
                        summary.setSpeciesBreakdown(speciesBreakdown);

                        // Generate animal type breakdown
                        Map<String, Long> animalTypeBreakdown = recentDetections.stream()
                                        .collect(Collectors.groupingBy(
                                                        AnimalDetection::getAnimalType,
                                                        Collectors.counting()));
                        summary.setAnimalTypeBreakdown(animalTypeBreakdown);

                        // Generate device breakdown
                        Map<String, Long> deviceBreakdown = recentDetections.stream()
                                        .filter(d -> d.getDeviceId() != null)
                                        .collect(Collectors.groupingBy(
                                                        AnimalDetection::getDeviceId,
                                                        Collectors.counting()));
                        summary.setDeviceBreakdown(deviceBreakdown);

                        // Find most active hour
                        Map<Integer, Long> hourlyCount = recentDetections.stream()
                                        .collect(Collectors.groupingBy(
                                                        d -> d.getTimestamp().getHour(),
                                                        Collectors.counting()));

                        Optional<Map.Entry<Integer, Long>> mostActiveEntry = hourlyCount.entrySet().stream()
                                        .max(Map.Entry.comparingByValue());

                        mostActiveEntry.ifPresent(integerLongEntry -> summary
                                        .setMostActiveHour(String.format("%02d:00", integerLongEntry.getKey())));

                        // Count detections in last hour
                        LocalDateTime lastHour = LocalDateTime.now().minusHours(1);
                        long lastHourCount = recentDetections.stream()
                                        .filter(d -> d.getTimestamp().isAfter(lastHour))
                                        .count();
                        summary.setDetectionsInLastHour(lastHourCount);

                        // Calculate type-specific counts
                        long birdCount = recentDetections.stream()
                                        .filter(AnimalDetection::isBird)
                                        .count();
                        summary.setBirdDetections(birdCount);

                        long mammalCount = recentDetections.stream()
                                        .filter(AnimalDetection::isMammal)
                                        .count();
                        summary.setMammalDetections(mammalCount);

                        long otherCount = recentDetections.size() - birdCount - mammalCount;
                        summary.setOtherAnimalDetections(otherCount);

                        log.info("Generated summary for {} detections: {} unique species, {} animal types, avg confidence: {:.2f}",
                                        recentDetections.size(), uniqueSpecies.size(), uniqueAnimalTypes.size(),
                                        avgConfidence);
                }

                return summary;
        }

        /**
         * Generates species count statistics for a given timeframe.
         *
         * @param hours Number of hours to analyze
         * @return Map of species names to detection counts
         */
        public Map<String, Long> getSpeciesCount(int hours) {
                log.debug("Generating species count for last {} hours", hours);
                LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
                List<AnimalDetection> recentDetections = animalDetectionRepository.findRecentDetections(startTime);

                return recentDetections.stream()
                                .collect(Collectors.groupingBy(
                                                AnimalDetection::getSpecies,
                                                Collectors.counting()));
        }

        /**
         * Generates animal type count statistics for a given timeframe.
         *
         * @param hours Number of hours to analyze
         * @return Map of animal types to detection counts
         */
        public Map<String, Long> getAnimalTypeCount(int hours) {
                log.debug("Generating animal type count for last {} hours", hours);
                LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
                List<AnimalDetection> recentDetections = animalDetectionRepository.findRecentDetections(startTime);

                return recentDetections.stream()
                                .collect(Collectors.groupingBy(
                                                AnimalDetection::getAnimalType,
                                                Collectors.counting()));
        }

        /**
         * Generates hourly activity distribution for a specific date.
         *
         * @param date The date to analyze
         * @return Map of hour (0-23) to detection counts
         */
        public Map<Integer, Long> getHourlyActivity(LocalDate date) {
                log.debug("Generating hourly activity for date: {}", date);
                LocalDateTime startOfDay = date.atStartOfDay();
                LocalDateTime endOfDay = date.atTime(23, 59, 59);
                List<AnimalDetection> dayDetections = animalDetectionRepository
                                .findByTimestampBetweenOrderByTimestampDesc(startOfDay, endOfDay);

                return dayDetections.stream()
                                .collect(Collectors.groupingBy(
                                                d -> d.getTimestamp().getHour(),
                                                Collectors.counting()));
        }

        /**
         * Generates comprehensive system-wide statistics and metrics.
         *
         * @return Map containing various system statistics and metrics
         */
        public Map<String, Object> getSystemStats() {
                log.debug("Generating comprehensive system statistics");
                Map<String, Object> stats = new HashMap<>();

                // Calculate total detections
                long totalDetections = animalDetectionRepository.count();
                stats.put("totalDetections", totalDetections);

                // Calculate today's detections
                LocalDate today = LocalDate.now();
                List<AnimalDetection> todayDetections = getDetectionsByDate(today);
                stats.put("detectionsToday", todayDetections.size());

                // Calculate this week's detections
                LocalDateTime weekStart = LocalDateTime.now().minusDays(7);
                List<AnimalDetection> weekDetections = animalDetectionRepository.findRecentDetections(weekStart);
                stats.put("detectionsThisWeek", weekDetections.size());

                // Find most detected species (all time)
                List<AnimalDetection> allDetections = animalDetectionRepository.findAll();
                Map<String, Long> allSpeciesCount = allDetections.stream()
                                .collect(Collectors.groupingBy(
                                                AnimalDetection::getSpecies,
                                                Collectors.counting()));

                Optional<Map.Entry<String, Long>> mostDetectedSpecies = allSpeciesCount.entrySet().stream()
                                .max(Map.Entry.comparingByValue());

                if (mostDetectedSpecies.isPresent()) {
                        stats.put("mostDetectedSpecies", mostDetectedSpecies.get().getKey());
                        stats.put("mostDetectedSpeciesCount", mostDetectedSpecies.get().getValue());
                }

                // Calculate unique species and animal types count
                long uniqueSpeciesCount = allSpeciesCount.size();
                stats.put("uniqueSpeciesCount", uniqueSpeciesCount);

                Map<String, Long> allAnimalTypeCount = allDetections.stream()
                                .collect(Collectors.groupingBy(
                                                AnimalDetection::getAnimalType,
                                                Collectors.counting()));
                stats.put("uniqueAnimalTypes", allAnimalTypeCount.size());
                stats.put("animalTypeBreakdown", allAnimalTypeCount);

                // Calculate average confidence
                double avgConfidence = allDetections.stream()
                                .mapToDouble(AnimalDetection::getConfidence)
                                .average()
                                .orElse(0.0);
                stats.put("averageConfidence", Math.round(avgConfidence * 100.0) / 100.0);

                // Count active devices
                Set<String> activeDevices = allDetections.stream()
                                .map(AnimalDetection::getDeviceId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet());
                stats.put("activeDevices", activeDevices.size());

                // Find first and last detection times
                Optional<AnimalDetection> firstDetection = allDetections.stream()
                                .min(Comparator.comparing(AnimalDetection::getTimestamp));
                Optional<AnimalDetection> lastDetection = allDetections.stream()
                                .max(Comparator.comparing(AnimalDetection::getTimestamp));

                firstDetection.ifPresent(d -> stats.put("firstDetection", d.getTimestamp()));
                lastDetection.ifPresent(d -> stats.put("lastDetection", d.getTimestamp()));

                // Count high confidence detections
                long highConfidenceCount = allDetections.stream()
                                .filter(d -> d.getConfidence() >= 0.8f)
                                .count();
                stats.put("highConfidenceDetections", highConfidenceCount);

                // Calculate detection rate
                if (firstDetection.isPresent()) {
                        long daysSinceFirst = java.time.temporal.ChronoUnit.DAYS.between(
                                        firstDetection.get().getTimestamp().toLocalDate(),
                                        LocalDate.now()) + 1;
                        double detectionsPerDay = (double) totalDetections / daysSinceFirst;
                        stats.put("averageDetectionsPerDay", Math.round(detectionsPerDay * 100.0) / 100.0);
                }

                // Add backward compatibility stats for birds
                long birdDetections = allDetections.stream().filter(AnimalDetection::isBird).count();
                stats.put("birdDetections", birdDetections);

                long mammalDetections = allDetections.stream().filter(AnimalDetection::isMammal).count();
                stats.put("mammalDetections", mammalDetections);

                log.info("Generated system stats - Total: {}, Today: {}, Week: {}, Species: {}, Animal Types: {}",
                                totalDetections, todayDetections.size(), weekDetections.size(), uniqueSpeciesCount,
                                allAnimalTypeCount.size());

                return stats;
        }

        /**
         * Retrieves all distinct animal species from the database.
         *
         * @return List of unique species names sorted alphabetically
         */
        public List<String> getAllSpecies() {
                log.debug("Retrieving all distinct species");
                return animalDetectionRepository.findAllDistinctSpecies();
        }

        /**
         * Retrieves all distinct animal types from the database.
         *
         * @return List of unique animal types sorted alphabetically
         */
        public List<String> getAllAnimalTypes() {
                log.debug("Retrieving all distinct animal types");
                return animalDetectionRepository.findAllDistinctAnimalTypes();
        }

        /**
         * Retrieves all distinct device identifiers from the database.
         *
         * @return List of unique device IDs sorted alphabetically
         */
        public List<String> getAllDevices() {
                log.debug("Retrieving all distinct devices");
                return animalDetectionRepository.findAllDistinctDeviceIds();
        }

        // Backward compatibility methods for bird detection
        /**
         * Retrieves bird detections specifically (backward compatibility).
         *
         * @param hours Number of hours to look back
         * @return List of bird detections
         */
        public List<AnimalDetection> getBirdDetections(int hours) {
                log.debug("Retrieving bird detections from the last {} hours", hours);
                LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
                return animalDetectionRepository.findBirdDetections(startTime);
        }

        /**
         * Retrieves mammal detections specifically.
         *
         * @param hours Number of hours to look back
         * @return List of mammal detections
         */
        public List<AnimalDetection> getMammalDetections(int hours) {
                log.debug("Retrieving mammal detections from the last {} hours", hours);
                LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
                return animalDetectionRepository.findMammalDetections(startTime);
        }
}