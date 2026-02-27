package org.example.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.backend.dto.AnimalDetectionDTO;
import org.example.backend.dto.AnimalDetectionSummary;
import org.example.backend.mapper.AnimalDetectionMapper;
import org.example.backend.model.AnimalDetection;
import org.example.backend.service.AnimalDetectionCommandService;
import org.example.backend.service.AnimalDetectionQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST controller for animal detection management and analytics.
 * Uses CQRS pattern with separate Command and Query services.
 * Provides endpoints for retrieving detection data, species analytics, and
 * device statistics
 * from computer vision systems and IoT animal monitoring devices.
 * Supports multiple animal types including birds, mammals, and other wildlife.
 */
@RestController
@RequestMapping("/api/animals")
@RequiredArgsConstructor
@Tag(name = "Animal Detection", description = "API for managing and analyzing animal detection data")
public class AnimalDetectionController {

        private static final Logger logger = LoggerFactory.getLogger(AnimalDetectionController.class);

        private final AnimalDetectionCommandService commandService;
        private final AnimalDetectionQueryService queryService;
        private final AnimalDetectionMapper mapper;

        /**
         * Retrieves the most recent animal detections.
         * 
         * @param limit Maximum number of detections to return (default: 10)
         * @return List of latest animal detections
         */
        @Operation(summary = "Get latest animal detections", description = "Retrieves the most recent animal detections with a configurable limit", tags = {
                        "Read" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved latest detections", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = AnimalDetectionDTO.class))))
        })
        @GetMapping("/latest")
        public ResponseEntity<List<AnimalDetectionDTO>> getLatestDetections(
                        @Parameter(description = "Maximum number of detections to return") @RequestParam(defaultValue = "10") int limit) {
                logger.info("Retrieving {} latest animal detections", limit);
                List<AnimalDetection> detections = queryService.getLatestDetections(limit);
                List<AnimalDetectionDTO> dtos = mapper.toDTOList(detections);
                logger.debug("Retrieved {} latest detections", dtos.size());
                return ResponseEntity.ok(dtos);
        }

        /**
         * Retrieves animal detections from the specified number of hours.
         * 
         * @param hours Number of hours to look back (default: 24)
         * @return List of recent animal detections
         */
        @Operation(summary = "Get recent animal detections", description = "Retrieves animal detections from the specified number of hours in the past", tags = {
                        "Read" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved recent detections", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = AnimalDetectionDTO.class))))
        })
        @GetMapping("/recent")
        public ResponseEntity<List<AnimalDetectionDTO>> getRecentDetections(
                        @Parameter(description = "Number of hours to look back") @RequestParam(defaultValue = "24") int hours) {
                logger.info("Retrieving animal detections from the last {} hours", hours);
                List<AnimalDetection> detections = queryService.getRecentDetections(hours);
                List<AnimalDetectionDTO> dtos = mapper.toDTOList(detections);
                logger.debug("Retrieved {} recent detections", dtos.size());
                return ResponseEntity.ok(dtos);
        }

        /**
         * Retrieves all animal detections for a specific date.
         * 
         * @param date Target date for detections
         * @return List of animal detections for the specified date
         */
        @Operation(summary = "Get detections by date", description = "Retrieves all animal detections for a specific date", tags = {
                        "Read" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved detections for date", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = AnimalDetectionDTO.class))))
        })
        @GetMapping("/by-date")
        public ResponseEntity<List<AnimalDetectionDTO>> getDetectionsByDate(
                        @Parameter(description = "Target date for detections (ISO format: yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
                logger.info("Retrieving animal detections for date: {}", date);
                List<AnimalDetection> detections = queryService.getDetectionsByDate(date);
                List<AnimalDetectionDTO> dtos = mapper.toDTOList(detections);
                logger.debug("Retrieved {} detections for date {}", dtos.size(), date);
                return ResponseEntity.ok(dtos);
        }

        /**
         * Retrieves animal detections for a specific species with pagination.
         * 
         * @param species Animal species name to filter by
         * @param page    Page number (default: 0)
         * @param size    Page size (default: 20)
         * @return Paginated list of detections for the specified species
         */
        @Operation(summary = "Get detections by species", description = "Retrieves animal detections for a specific species with pagination", tags = {
                        "Read" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved detections for species", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = AnimalDetectionDTO.class))))
        })
        @GetMapping("/by-species")
        public ResponseEntity<List<AnimalDetectionDTO>> getDetectionsBySpecies(
                        @Parameter(description = "Animal species name to filter by") @RequestParam String species,
                        @Parameter(description = "Page number (zero-based)") @RequestParam(defaultValue = "0") int page,
                        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
                logger.info("Retrieving detections for species '{}' (page {}, size {})", species, page, size);
                Pageable pageable = PageRequest.of(page, size);
                List<AnimalDetection> detections = queryService.getDetectionsBySpecies(species, pageable);
                List<AnimalDetectionDTO> dtos = mapper.toDTOList(detections);
                logger.debug("Retrieved {} detections for species '{}'", dtos.size(), species);
                return ResponseEntity.ok(dtos);
        }

        /**
         * Retrieves animal detections for a specific animal type with pagination.
         * 
         * @param animalType Animal type to filter by (e.g., "bird", "mammal")
         * @param page       Page number (default: 0)
         * @param size       Page size (default: 20)
         * @return Paginated list of detections for the specified animal type
         */
        @Operation(summary = "Get detections by animal type", description = "Retrieves animal detections for a specific animal type (e.g., 'bird', 'mammal') with pagination", tags = {
                        "Read" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved detections for animal type", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = AnimalDetectionDTO.class))))
        })
        @GetMapping("/by-type")
        public ResponseEntity<List<AnimalDetectionDTO>> getDetectionsByAnimalType(
                        @Parameter(description = "Animal type to filter by (e.g., 'bird', 'mammal')") @RequestParam String animalType,
                        @Parameter(description = "Page number (zero-based)") @RequestParam(defaultValue = "0") int page,
                        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
                logger.info("Retrieving detections for animal type '{}' (page {}, size {})", animalType, page, size);
                Pageable pageable = PageRequest.of(page, size);
                List<AnimalDetection> detections = queryService.getDetectionsByAnimalType(animalType,
                                pageable);
                List<AnimalDetectionDTO> dtos = mapper.toDTOList(detections);
                logger.debug("Retrieved {} detections for animal type '{}'", dtos.size(), animalType);
                return ResponseEntity.ok(dtos);
        }

        /**
         * Retrieves animal detections for a specific device with pagination.
         * 
         * @param deviceId Device identifier to filter by
         * @param page     Page number (default: 0)
         * @param size     Page size (default: 20)
         * @return Paginated list of detections for the specified device
         */
        @Operation(summary = "Get detections by device", description = "Retrieves animal detections for a specific device with pagination")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved detections for device", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = AnimalDetectionDTO.class))))
        })
        @GetMapping("/by-device")
        public ResponseEntity<List<AnimalDetectionDTO>> getDetectionsByDevice(
                        @Parameter(description = "Device identifier to filter by") @RequestParam String deviceId,
                        @Parameter(description = "Page number (zero-based)") @RequestParam(defaultValue = "0") int page,
                        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
                logger.info("Retrieving detections for device '{}' (page {}, size {})", deviceId, page, size);
                Pageable pageable = PageRequest.of(page, size);
                List<AnimalDetection> detections = queryService.getDetectionsByDevice(deviceId, pageable);
                List<AnimalDetectionDTO> dtos = mapper.toDTOList(detections);
                logger.debug("Retrieved {} detections for device '{}'", dtos.size(), deviceId);
                return ResponseEntity.ok(dtos);
        }

        /**
         * Retrieves comprehensive detection analytics and summary statistics.
         * 
         * @param hours Time period in hours for analysis (default: 24)
         * @return Detection summary with statistics and breakdowns
         */
        @Operation(summary = "Get detection summary", description = "Retrieves comprehensive detection analytics and summary statistics for a time period")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved detection summary", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AnimalDetectionSummary.class)))
        })
        @GetMapping("/summary")
        public ResponseEntity<AnimalDetectionSummary> getDetectionSummary(
                        @Parameter(description = "Time period in hours for analysis") @RequestParam(defaultValue = "24") int hours) {
                logger.info("Retrieving detection summary for the last {} hours", hours);
                AnimalDetectionSummary summary = queryService.getDetectionSummary(hours);
                logger.debug("Generated summary with {} total detections", summary.getTotalDetections());
                return ResponseEntity.ok(summary);
        }

        /**
         * Retrieves species detection counts for analytics.
         * 
         * @param hours Time period in hours for analysis (default: 24)
         * @return Map of species names to detection counts
         */
        @Operation(summary = "Get species count", description = "Retrieves detection counts grouped by species for analytics")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved species counts", content = @Content(mediaType = "application/json"))
        })
        @GetMapping("/species-count")
        public ResponseEntity<Map<String, Long>> getSpeciesCount(
                        @Parameter(description = "Time period in hours for analysis") @RequestParam(defaultValue = "24") int hours) {
                logger.info("Retrieving species counts for the last {} hours", hours);
                Map<String, Long> speciesCount = queryService.getSpeciesCount(hours);
                logger.debug("Retrieved species counts for {} different species", speciesCount.size());
                return ResponseEntity.ok(speciesCount);
        }

        /**
         * Retrieves animal type detection counts for analytics.
         * 
         * @param hours Time period in hours for analysis (default: 24)
         * @return Map of animal types to detection counts
         */
        @Operation(summary = "Get animal type count", description = "Retrieves detection counts grouped by animal type for analytics")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved animal type counts", content = @Content(mediaType = "application/json"))
        })
        @GetMapping("/type-count")
        public ResponseEntity<Map<String, Long>> getAnimalTypeCount(
                        @Parameter(description = "Time period in hours for analysis") @RequestParam(defaultValue = "24") int hours) {
                logger.info("Retrieving animal type counts for the last {} hours", hours);
                Map<String, Long> typeCount = queryService.getAnimalTypeCount(hours);
                logger.debug("Retrieved animal type counts for {} different types", typeCount.size());
                return ResponseEntity.ok(typeCount);
        }

        /**
         * Retrieves hourly detection activity patterns for a specific date.
         * 
         * @param date Target date for activity analysis
         * @return Map of hour (0-23) to detection counts
         */
        @Operation(summary = "Get hourly activity", description = "Retrieves hourly detection activity patterns for a specific date")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved hourly activity data", content = @Content(mediaType = "application/json"))
        })
        @GetMapping("/hourly-activity")
        public ResponseEntity<Map<Integer, Long>> getHourlyActivity(
                        @Parameter(description = "Target date for activity analysis (ISO format: yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
                logger.info("Retrieving hourly activity for date: {}", date);
                Map<Integer, Long> hourlyActivity = queryService.getHourlyActivity(date);
                logger.debug("Retrieved hourly activity data for {} hours", hourlyActivity.size());
                return ResponseEntity.ok(hourlyActivity);
        }

        /**
         * Retrieves high-confidence animal detections above a specified threshold.
         * 
         * @param minConfidence Minimum confidence score (default: 0.8)
         * @param hours         Time period in hours (default: 24)
         * @return List of high-confidence detections
         */
        @Operation(summary = "Get high-confidence detections", description = "Retrieves animal detections with confidence scores above a specified threshold")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved high-confidence detections", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = AnimalDetectionDTO.class))))
        })
        @GetMapping("/high-confidence")
        public ResponseEntity<List<AnimalDetectionDTO>> getHighConfidenceDetections(
                        @Parameter(description = "Minimum confidence score (0.0-1.0)") @RequestParam(defaultValue = "0.8") float minConfidence,
                        @Parameter(description = "Time period in hours") @RequestParam(defaultValue = "24") int hours) {
                logger.info("Retrieving high-confidence detections (min: {}) from last {} hours", minConfidence, hours);
                List<AnimalDetection> detections = queryService.getHighConfidenceDetections(minConfidence,
                                hours);
                List<AnimalDetectionDTO> dtos = mapper.toDTOList(detections);
                logger.debug("Retrieved {} high-confidence detections", dtos.size());
                return ResponseEntity.ok(dtos);
        }

        /**
         * Retrieves a specific animal detection by its ID.
         * 
         * @param id Detection ID
         * @return Animal detection details or 404 if not found
         */
        @Operation(summary = "Get detection by ID", description = "Retrieves a specific animal detection by its ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved the detection", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AnimalDetectionDTO.class))),
                        @ApiResponse(responseCode = "404", description = "Detection not found", content = @Content)
        })
        @GetMapping("/{id}")
        public ResponseEntity<AnimalDetectionDTO> getDetectionById(
                        @Parameter(description = "Detection ID") @PathVariable Long id) {
                logger.info("Retrieving detection with ID: {}", id);
                return queryService.getDetectionById(id)
                                .map(detection -> {
                                        logger.debug("Found detection with ID: {}", id);
                                        return mapper.toDTO(detection);
                                })
                                .map(ResponseEntity::ok)
                                .orElseGet(() -> {
                                        logger.debug("Detection not found with ID: {}", id);
                                        return ResponseEntity.notFound().build();
                                });
        }

        /**
         * Records a new animal detection event in the system.
         * 
         * @param detectionDTO Animal detection data to be stored
         * @return Saved detection with generated ID and timestamps
         */
        @Operation(summary = "Record new detection", description = "Records a new animal detection event in the system")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully recorded the detection", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AnimalDetectionDTO.class)))
        })
        @PostMapping("/detect")
        public ResponseEntity<AnimalDetectionDTO> recordDetection(
                        @Parameter(description = "Animal detection data", required = true) @RequestBody AnimalDetectionDTO detectionDTO) {
                logger.info("Recording new animal detection - Type: {}, Species: {}",
                                detectionDTO.getAnimalType(), detectionDTO.getSpecies());
                AnimalDetection detection = mapper.toEntity(detectionDTO);
                AnimalDetection savedDetection = commandService.saveAnimalDetection(detection);
                AnimalDetectionDTO savedDTO = mapper.toDTO(savedDetection);
                logger.debug("Saved detection with ID: {}", savedDTO.getId());
                return ResponseEntity.ok(savedDTO);
        }

        /**
         * Deletes a specific animal detection by its ID.
         * 
         * @param id Detection ID to delete
         * @return 204 No Content if successful, 404 if not found
         */
        @Operation(summary = "Delete detection", description = "Deletes a specific animal detection by its ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "204", description = "Successfully deleted the detection", content = @Content),
                        @ApiResponse(responseCode = "404", description = "Detection not found", content = @Content)
        })
        @DeleteMapping("/{id}")
        public ResponseEntity<Void> deleteDetection(
                        @Parameter(description = "Detection ID to delete") @PathVariable Long id) {
                logger.info("Deleting detection with ID: {}", id);
                if (commandService.deleteDetection(id)) {
                        logger.debug("Successfully deleted detection with ID: {}", id);
                        return ResponseEntity.noContent().build();
                }
                logger.debug("Detection not found for deletion with ID: {}", id);
                return ResponseEntity.notFound().build();
        }

        /**
         * Retrieves comprehensive system statistics and metrics.
         * 
         * @return Map containing various system statistics
         */
        @Operation(summary = "Get system statistics", description = "Retrieves comprehensive system statistics and metrics about animal detections")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved system statistics", content = @Content(mediaType = "application/json"))
        })
        @GetMapping("/stats")
        public ResponseEntity<Map<String, Object>> getSystemStats() {
                logger.info("Retrieving system statistics");
                Map<String, Object> stats = queryService.getSystemStats();
                logger.debug("Retrieved system statistics with {} metrics", stats.size());
                return ResponseEntity.ok(stats);
        }

        /**
         * Retrieves all distinct species in the system.
         * 
         * @return List of unique species names
         */
        @Operation(summary = "Get all species", description = "Retrieves all distinct species names in the system")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved species list", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = String.class))))
        })
        @GetMapping("/species")
        public ResponseEntity<List<String>> getAllSpecies() {
                logger.info("Retrieving all distinct species");
                List<String> species = queryService.getAllSpecies();
                logger.debug("Retrieved {} distinct species", species.size());
                return ResponseEntity.ok(species);
        }

        /**
         * Retrieves all distinct animal types in the system.
         * 
         * @return List of unique animal types
         */
        @Operation(summary = "Get all animal types", description = "Retrieves all distinct animal types in the system")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved animal types list", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = String.class))))
        })
        @GetMapping("/types")
        public ResponseEntity<List<String>> getAllAnimalTypes() {
                logger.info("Retrieving all distinct animal types");
                List<String> animalTypes = queryService.getAllAnimalTypes();
                logger.debug("Retrieved {} distinct animal types", animalTypes.size());
                return ResponseEntity.ok(animalTypes);
        }

        /**
         * Retrieves mammal detections specifically.
         * 
         * @param hours Number of hours to look back (default: 24)
         * @return List of mammal detections
         */
        @Operation(summary = "Get mammal detections", description = "Retrieves detections specifically for mammals")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved mammal detections", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = AnimalDetectionDTO.class))))
        })
        @GetMapping("/mammals")
        public ResponseEntity<List<AnimalDetectionDTO>> getMammalDetections(
                        @Parameter(description = "Number of hours to look back") @RequestParam(defaultValue = "24") int hours) {
                logger.info("Retrieving mammal detections from the last {} hours", hours);
                List<AnimalDetection> detections = queryService.getMammalDetections(hours);
                List<AnimalDetectionDTO> dtos = mapper.toDTOList(detections);
                logger.debug("Retrieved {} mammal detections", dtos.size());
                return ResponseEntity.ok(dtos);
        }
}