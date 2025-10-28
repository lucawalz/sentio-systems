package org.example.backend.repository;

import org.example.backend.model.LocationData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LocationDataRepository extends JpaRepository<LocationData, Long> {

    Optional<LocationData> findByIpAddress(String ipAddress);

    @Query("SELECT l FROM LocationData l WHERE l.ipAddress = :ipAddress ORDER BY l.updatedAt DESC")
    Optional<LocationData> findLatestByIpAddress(@Param("ipAddress") String ipAddress);

    @Query("SELECT l FROM LocationData l WHERE l.createdAt >= :startDate ORDER BY l.createdAt DESC")
    List<LocationData> findRecentLocationData(@Param("startDate") LocalDateTime startDate);

    @Modifying
    @Query("DELETE FROM LocationData l WHERE l.createdAt < :cutoffDate")
    void deleteOldLocationData(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT DISTINCT l.city FROM LocationData l WHERE l.createdAt >= :startDate")
    List<String> findDistinctCitiesSince(@Param("startDate") LocalDateTime startDate);

    List<LocationData> findByCity(String city);

    List<LocationData> findByCountry(String country);

    @Query("SELECT l FROM LocationData l WHERE l.latitude BETWEEN :latMin AND :latMax AND l.longitude BETWEEN :lonMin AND :lonMax")
    List<LocationData> findByCoordinateRange(@Param("latMin") Float latMin, @Param("latMax") Float latMax,
                                             @Param("lonMin") Float lonMin, @Param("lonMax") Float lonMax);
}