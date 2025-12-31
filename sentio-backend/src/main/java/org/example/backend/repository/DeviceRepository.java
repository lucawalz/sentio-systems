package org.example.backend.repository;

import org.example.backend.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, String> {

    /**
     * Find all devices owned by a specific user.
     */
    List<Device> findAllByOwnerId(String ownerId);

    @Override
    @org.springframework.lang.NonNull
    Optional<Device> findById(@org.springframework.lang.NonNull String id);
}
