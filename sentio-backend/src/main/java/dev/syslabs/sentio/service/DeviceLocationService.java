package dev.syslabs.sentio.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import dev.syslabs.sentio.event.DeviceLocationUpdatedEvent;
import dev.syslabs.sentio.model.Device;
import dev.syslabs.sentio.model.LocationData;
import dev.syslabs.sentio.repository.DeviceRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsible for managing device geographic locations.
 * Uses device GPS coordinates (latitude/longitude) directly.
 * Publishes events when locations change to trigger weather data fetching.
 *
 * @author Sentio Team
 * @version 2.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceLocationService {

    private final DeviceRepository deviceRepository;
    private final DeviceService deviceService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Helper to build LocationData from device coordinates.
     */
    private LocationData buildLocationDataFromDevice(Device device) {
        LocationData location = new LocationData();
        location.setLatitude(device.getLatitude() != null ? device.getLatitude().floatValue() : null);
        location.setLongitude(device.getLongitude() != null ? device.getLongitude().floatValue() : null);
        location.setDeviceId(device.getId());
        location.setCity("GPS Location");
        location.setCountry("Unknown");
        location.setCreatedAt(LocalDateTime.now());
        location.setUpdatedAt(LocalDateTime.now());
        return location;
    }

    private boolean hasValidCoordinates(Device device) {
        return device.getLatitude() != null && device.getLongitude() != null;
    }

    /**
     * Retrieves locations for all devices with GPS coordinates.
     */
    public List<LocationData> getAllUniqueDeviceLocations() {
        log.debug("Retrieving locations for all registered devices");

        List<Device> allDevices = deviceRepository.findAll();
        if (allDevices.isEmpty()) {
            log.debug("No devices registered in the system");
            return Collections.emptyList();
        }

        List<LocationData> locations = allDevices.stream()
                .filter(this::hasValidCoordinates)
                .map(this::buildLocationDataFromDevice)
                .collect(Collectors.toList());

        log.debug("Found {} devices with GPS coordinates", locations.size());
        return locations;
    }

    /**
     * Retrieves locations for current user's devices with GPS coordinates.
     */
    public List<LocationData> getCurrentUserDeviceLocations() {
        log.debug("Retrieving locations for current user's devices");

        List<Device> userDevices = deviceService.getMyDevices();
        if (userDevices.isEmpty()) {
            log.debug("Current user has no registered devices");
            return Collections.emptyList();
        }

        List<LocationData> locations = userDevices.stream()
                .filter(this::hasValidCoordinates)
                .map(this::buildLocationDataFromDevice)
                .collect(Collectors.toList());

        log.debug("Found {} user devices with GPS coordinates", locations.size());
        return locations;
    }

    /**
     * Retrieves all devices with their locations for the current user.
     */
    public Map<String, LocationData> getCurrentUserDevicesWithLocations() {
        log.debug("Retrieving devices with locations for current user");

        List<Device> userDevices = deviceService.getMyDevices();
        if (userDevices.isEmpty()) {
            log.debug("Current user has no registered devices");
            return Collections.emptyMap();
        }

        Map<String, LocationData> deviceLocations = new LinkedHashMap<>();
        for (Device device : userDevices) {
            if (hasValidCoordinates(device)) {
                deviceLocations.put(device.getId(), buildLocationDataFromDevice(device));
            }
        }

        log.debug("Retrieved {} device locations for current user", deviceLocations.size());
        return deviceLocations;
    }

    /**
     * Retrieves location for a specific device.
     */
    public Optional<LocationData> getDeviceLocation(String deviceId) {
        log.debug("Retrieving location for device: {}", deviceId);

        return deviceRepository.findById(deviceId)
                .filter(this::hasValidCoordinates)
                .map(this::buildLocationDataFromDevice);
    }

    /**
     * Gets the primary device location for the current user.
     * Falls back to first device with coordinates if no primary is set.
     */
    public Optional<LocationData> getPrimaryUserDeviceLocation() {
        log.debug("Retrieving primary device location for current user");

        List<Device> userDevices = deviceService.getMyDevices();
        if (userDevices.isEmpty()) {
            log.debug("No devices available for current user");
            return Optional.empty();
        }

        // First try to find explicitly set primary device
        Optional<Device> primary = userDevices.stream()
                .filter(d -> Boolean.TRUE.equals(d.getIsPrimary()) && hasValidCoordinates(d))
                .findFirst();

        if (primary.isPresent()) {
            log.debug("Found primary device: {}", primary.get().getId());
            return Optional.of(buildLocationDataFromDevice(primary.get()));
        }

        // Fallback to first device with coordinates
        return userDevices.stream()
                .filter(this::hasValidCoordinates)
                .findFirst()
                .map(device -> {
                    log.debug("Using fallback device (no primary set): {}", device.getId());
                    return buildLocationDataFromDevice(device);
                });
    }

    /**
     * Gets the first available device location for the current user.
     * 
     * @deprecated Use {@link #getPrimaryUserDeviceLocation()} instead
     */
    @Deprecated
    public Optional<LocationData> getFirstUserDeviceLocation() {
        return getPrimaryUserDeviceLocation();
    }

    /**
     * Updates a device's GPS coordinates and publishes an event.
     * This triggers weather data fetching via DeviceEventListener.
     *
     * @param deviceId  The device ID
     * @param latitude  GPS latitude
     * @param longitude GPS longitude
     * @return true if updated successfully
     */
    public boolean updateDeviceGpsLocation(String deviceId, Double latitude, Double longitude) {
        log.debug("Updating device {} with GPS coordinates: ({}, {})", deviceId, latitude, longitude);

        Optional<Device> deviceOpt = deviceRepository.findById(deviceId);
        if (deviceOpt.isEmpty()) {
            log.warn("Device not found: {}", deviceId);
            return false;
        }

        Device device = deviceOpt.get();
        boolean isFirstLocation = !hasValidCoordinates(device);

        device.setLatitude(latitude);
        device.setLongitude(longitude);
        deviceRepository.save(device);

        log.info("Updated device {} GPS location: ({}, {})", deviceId, latitude, longitude);

        // Publish event to trigger weather data fetching
        eventPublisher.publishEvent(new DeviceLocationUpdatedEvent(
                this, deviceId, latitude, longitude, isFirstLocation));

        return true;
    }
}
