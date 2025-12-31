package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.model.Device;
import org.example.backend.model.LocationData;
import org.example.backend.repository.DeviceRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsible for managing the relationship between devices and their
 * geographic locations.
 * Provides centralized logic for retrieving location data associated with
 * registered devices.
 * <p>
 * This service enforces the strict device-only location policy:
 * - Only uses IP addresses from registered devices
 * - Never uses server or browser IP addresses
 * - Gracefully handles scenarios with no registered devices
 * </p>
 *
 * @author Sentio Team
 * @version 1.0
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceLocationService {

    private final DeviceRepository deviceRepository;
    private final DeviceService deviceService;
    private final IpLocationService ipLocationService;

    /**
     * Retrieves all unique geographic locations for all registered devices.
     * This method queries all devices in the system and resolves their locations
     * based on their registered IP addresses.
     *
     * @return List of unique LocationData objects for all devices. Returns empty
     *         list if no devices registered.
     */
    public List<LocationData> getAllUniqueDeviceLocations() {
        log.debug("Retrieving locations for all registered devices");

        List<Device> allDevices = deviceRepository.findAll();

        if (allDevices.isEmpty()) {
            log.debug("No devices registered in the system");
            return Collections.emptyList();
        }

        log.info("Found {} registered devices, resolving their locations", allDevices.size());

        // Collect unique IP addresses from devices
        Set<String> uniqueIps = allDevices.stream()
                .map(Device::getIpAddress)
                .filter(Objects::nonNull)
                .filter(ip -> !ip.isEmpty())
                .collect(Collectors.toSet());

        if (uniqueIps.isEmpty()) {
            log.warn("No devices have IP addresses registered yet");
            return Collections.emptyList();
        }

        log.debug("Found {} unique IP addresses from devices", uniqueIps.size());

        // Resolve locations for each unique IP
        List<LocationData> locations = uniqueIps.stream()
                .map(ipLocationService::getLocationByIp)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        log.info("Successfully resolved {} unique device locations", locations.size());
        return locations;
    }

    /**
     * Retrieves all unique geographic locations for devices owned by the current
     * user.
     * Filters devices by the authenticated user's ownership before resolving
     * locations.
     *
     * @return List of unique LocationData objects for user's devices. Returns empty
     *         list if user has no devices.
     */
    public List<LocationData> getCurrentUserDeviceLocations() {
        log.debug("Retrieving locations for current user's devices");

        List<Device> userDevices = deviceService.getMyDevices();

        if (userDevices.isEmpty()) {
            log.debug("Current user has no registered devices");
            return Collections.emptyList();
        }

        log.info("Current user has {} registered devices", userDevices.size());

        // Collect unique IP addresses from user's devices
        Set<String> uniqueIps = userDevices.stream()
                .map(Device::getIpAddress)
                .filter(Objects::nonNull)
                .filter(ip -> !ip.isEmpty())
                .collect(Collectors.toSet());

        if (uniqueIps.isEmpty()) {
            log.warn("User's devices have no IP addresses registered yet");
            return Collections.emptyList();
        }

        log.debug("Found {} unique IP addresses from user's devices", uniqueIps.size());

        // Resolve locations for each unique IP
        List<LocationData> locations = uniqueIps.stream()
                .map(ipLocationService::getLocationByIp)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        log.info("Successfully resolved {} unique locations for current user", locations.size());
        return locations;
    }

    /**
     * Retrieves all devices with their locations for the current user.
     * Returns a map of device ID to LocationData for user data isolation.
     *
     * @return Map of deviceId to LocationData for user's devices
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
            String ip = device.getIpAddress();
            if (ip != null && !ip.isEmpty()) {
                Optional<LocationData> location = ipLocationService.getLocationByIp(ip);
                if (location.isPresent()) {
                    LocationData locData = location.get();
                    locData.setDeviceId(device.getId());
                    deviceLocations.put(device.getId(), locData);
                    log.debug("Resolved location for device {}: {}, {}",
                            device.getId(), locData.getCity(), locData.getCountry());
                }
            }
        }

        log.info("Resolved {} device locations for current user", deviceLocations.size());
        return deviceLocations;
    }

    /**
     * Retrieves the geographic location for a specific device by its ID.
     *
     * @param deviceId The unique device identifier
     * @return Optional containing the device's LocationData if found, empty
     *         otherwise
     */
    public Optional<LocationData> getDeviceLocation(String deviceId) {
        log.debug("Retrieving location for device: {}", deviceId);

        Optional<Device> deviceOpt = deviceRepository.findById(deviceId);

        if (deviceOpt.isEmpty()) {
            log.warn("Device not found: {}", deviceId);
            return Optional.empty();
        }

        Device device = deviceOpt.get();
        String ipAddress = device.getIpAddress();

        if (ipAddress == null || ipAddress.isEmpty()) {
            log.warn("Device {} has no IP address registered", deviceId);
            return Optional.empty();
        }

        Optional<LocationData> location = ipLocationService.getLocationByIp(ipAddress);

        if (location.isPresent()) {
            log.info("Successfully resolved location for device {}: {}, {}",
                    deviceId, location.get().getCity(), location.get().getCountry());
        } else {
            log.warn("Could not resolve location for device {} with IP {}", deviceId, ipAddress);
        }

        return location;
    }

    /**
     * Gets the first available device location for the current user.
     * Useful for user-facing endpoints that need a single location.
     *
     * @return Optional containing the first device's LocationData, empty if user
     *         has no devices
     */
    public Optional<LocationData> getFirstUserDeviceLocation() {
        List<LocationData> locations = getCurrentUserDeviceLocations();

        if (locations.isEmpty()) {
            log.debug("No device locations available for current user");
            return Optional.empty();
        }

        LocationData firstLocation = locations.get(0);
        log.debug("Returning first device location: {}, {}",
                firstLocation.getCity(), firstLocation.getCountry());
        return Optional.of(firstLocation);
    }
}
