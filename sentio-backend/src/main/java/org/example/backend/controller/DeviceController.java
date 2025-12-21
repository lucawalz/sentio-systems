package org.example.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.model.Device;
import org.example.backend.service.DeviceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Device Management", description = "API for managing IoT devices")
public class DeviceController {

    private final DeviceService deviceService;

    @Operation(summary = "Register a device", description = "Register an existing embedded device to the current user")
    @PostMapping("/register")
    public ResponseEntity<Device> registerDevice(@RequestParam String deviceId,
            @RequestParam(defaultValue = "My Device") String name) {
        log.info("Request to register device: {}", deviceId);
        Device device = deviceService.registerDevice(deviceId, name);
        return ResponseEntity.ok(device);
    }

    @Operation(summary = "List my devices", description = "List all devices owned by the current user")
    @GetMapping
    public ResponseEntity<List<Device>> getMyDevices() {
        return ResponseEntity.ok(deviceService.getMyDevices());
    }

    @Operation(summary = "Unregister a device", description = "Remove the current user from the device owners")
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> unregisterDevice(@PathVariable String deviceId) {
        deviceService.unregisterDevice(deviceId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Check device registration", description = "Check if user has any registered devices. Use this before calling weather APIs.")
    @GetMapping("/has-any")
    public ResponseEntity<Boolean> hasAnyDevices() {
        return ResponseEntity.ok(deviceService.hasAnyDevices());
    }
}
