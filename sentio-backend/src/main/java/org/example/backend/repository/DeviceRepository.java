package org.example.backend.repository;

import org.example.backend.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, String> {

    /**
     * Find all devices owned by a specific user.
     * Since owners is an @ElementCollection, we can query it using member of or
     * join.
     */
    @Query("SELECT d FROM Device d WHERE :userId MEMBER OF d.owners")
    List<Device> findAllByOwnerId(@Param("userId") String userId);

    @Override
    @org.springframework.lang.NonNull
    Optional<Device> findById(@org.springframework.lang.NonNull String id);
}
