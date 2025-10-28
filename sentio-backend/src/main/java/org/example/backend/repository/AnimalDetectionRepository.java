
package org.example.backend.repository;

import org.example.backend.model.AnimalDetection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnimalDetectionRepository extends JpaRepository<AnimalDetection, Long> {

    @Query("SELECT a FROM AnimalDetection a WHERE a.timestamp >= :start ORDER BY a.timestamp DESC")
    List<AnimalDetection> findRecentDetections(@Param("start") LocalDateTime start);

    @Query(value = "SELECT * FROM animal_detections ORDER BY timestamp DESC LIMIT :limit", nativeQuery = true)
    List<AnimalDetection> findTopNByOrderByTimestampDesc(@Param("limit") int limit);

    List<AnimalDetection> findTop10ByOrderByTimestampDesc();

    List<AnimalDetection> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);

    List<AnimalDetection> findBySpeciesIgnoreCaseOrderByTimestampDesc(String species, Pageable pageable);

    List<AnimalDetection> findByAnimalTypeIgnoreCaseOrderByTimestampDesc(String animalType, Pageable pageable);

    List<AnimalDetection> findByDeviceIdOrderByTimestampDesc(String deviceId, Pageable pageable);

    @Query("SELECT a FROM AnimalDetection a WHERE a.confidence >= :minConfidence AND a.timestamp >= :start ORDER BY a.timestamp DESC")
    List<AnimalDetection> findHighConfidenceDetections(@Param("minConfidence") float minConfidence, @Param("start") LocalDateTime start);

    @Query("SELECT a FROM AnimalDetection a WHERE a.location = :location ORDER BY a.timestamp DESC")
    List<AnimalDetection> findByLocationOrderByTimestampDesc(@Param("location") String location);

    @Query("SELECT COUNT(DISTINCT a.species) FROM AnimalDetection a WHERE a.timestamp >= :start")
    long countUniqueSpeciesSince(@Param("start") LocalDateTime start);

    @Query("SELECT COUNT(DISTINCT a.animalType) FROM AnimalDetection a WHERE a.timestamp >= :start")
    long countUniqueAnimalTypesSince(@Param("start") LocalDateTime start);

    @Query("SELECT COUNT(DISTINCT a.deviceId) FROM AnimalDetection a WHERE a.deviceId IS NOT NULL")
    long countActiveDevices();

    @Query("SELECT a FROM AnimalDetection a WHERE a.species = :species AND a.timestamp >= :start ORDER BY a.timestamp DESC")
    List<AnimalDetection> findBySpeciesAndTimestampAfter(@Param("species") String species, @Param("start") LocalDateTime start);

    @Query("SELECT a FROM AnimalDetection a WHERE a.animalType = :animalType AND a.timestamp >= :start ORDER BY a.timestamp DESC")
    List<AnimalDetection> findByAnimalTypeAndTimestampAfter(@Param("animalType") String animalType, @Param("start") LocalDateTime start);

    @Query("SELECT a FROM AnimalDetection a WHERE a.deviceId = :deviceId AND a.timestamp >= :start ORDER BY a.timestamp DESC")
    List<AnimalDetection> findByDeviceIdAndTimestampAfter(@Param("deviceId") String deviceId, @Param("start") LocalDateTime start);

    @Query("SELECT AVG(a.confidence) FROM AnimalDetection a WHERE a.timestamp >= :start")
    Double findAverageConfidenceSince(@Param("start") LocalDateTime start);

    @Query("SELECT a FROM AnimalDetection a WHERE a.location = :location AND a.species = :species AND a.timestamp >= :start ORDER BY a.timestamp DESC")
    List<AnimalDetection> findByLocationAndSpeciesAndTimestampAfter(
            @Param("location") String location,
            @Param("species") String species,
            @Param("start") LocalDateTime start);

    @Query("SELECT DISTINCT a.species FROM AnimalDetection a ORDER BY a.species")
    List<String> findAllDistinctSpecies();

    @Query("SELECT DISTINCT a.animalType FROM AnimalDetection a ORDER BY a.animalType")
    List<String> findAllDistinctAnimalTypes();

    @Query("SELECT DISTINCT a.deviceId FROM AnimalDetection a WHERE a.deviceId IS NOT NULL ORDER BY a.deviceId")
    List<String> findAllDistinctDeviceIds();

    @Query("SELECT DISTINCT a.location FROM AnimalDetection a WHERE a.location IS NOT NULL ORDER BY a.location")
    List<String> findAllDistinctLocations();

    @Query("SELECT COUNT(a) FROM AnimalDetection a WHERE a.timestamp >= :start AND a.timestamp < :end")
    long countDetectionsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT a FROM AnimalDetection a WHERE a.confidence >= :confidence ORDER BY a.timestamp DESC")
    List<AnimalDetection> findByConfidenceGreaterThanEqualOrderByTimestampDesc(@Param("confidence") float confidence, Pageable pageable);

    // Bird specific queries
    @Query("SELECT a FROM AnimalDetection a WHERE a.animalType = 'bird' AND a.timestamp >= :start ORDER BY a.timestamp DESC")
    List<AnimalDetection> findBirdDetections(@Param("start") LocalDateTime start);

    @Query("SELECT COUNT(a) FROM AnimalDetection a WHERE a.animalType = 'bird' AND a.timestamp >= :start")
    long countBirdDetectionsSince(@Param("start") LocalDateTime start);

    // Mammal-specific queries
    @Query("SELECT a FROM AnimalDetection a WHERE a.animalType = 'mammal' AND a.timestamp >= :start ORDER BY a.timestamp DESC")
    List<AnimalDetection> findMammalDetections(@Param("start") LocalDateTime start);

    @Query("SELECT COUNT(a) FROM AnimalDetection a WHERE a.animalType = 'mammal' AND a.timestamp >= :start")
    long countMammalDetectionsSince(@Param("start") LocalDateTime start);
}