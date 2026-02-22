package org.example.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.model.AnimalDetection;
import org.example.backend.model.Device;
import org.example.backend.model.RaspiWeatherData;
import org.example.backend.repository.AnimalDetectionRepository;
import org.example.backend.repository.DeviceRepository;
import org.example.backend.repository.RaspiWeatherDataRepository;
import org.example.backend.service.ViewerSessionService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Seeds realistic demo data when the application starts with the {@code demo} profile.
 *
 * <p>Activate via environment variable or {@code .env}:
 * <pre>SPRING_PROFILES_ACTIVE=demo</pre>
 *
 * <p>Data is only seeded once. If devices already exist in the database the
 * initializer skips all inserts. This prevents duplicate data on restarts.
 *
 * <p>Demo data does <strong>not</strong> load in the {@code prod} profile.
 */
@Component
@Profile("demo")
@Slf4j
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final String DEVICE_1  = "demo-device-001";
    private static final String DEVICE_2  = "demo-device-002";
    private static final String OWNER_ID  = "demo-user-001";
    private static final String LOCATION_1 = "Garching, Bayern";
    private static final String LOCATION_2 = "Forstenried, München";

    private final DeviceRepository            deviceRepository;
    private final RaspiWeatherDataRepository  weatherDataRepository;
    private final AnimalDetectionRepository   animalDetectionRepository;
    private final ViewerSessionService        viewerSessionService;

    @Override
    public void run(ApplicationArguments args) {
        if (deviceRepository.count() > 0) {
            log.info("Demo data already present — skipping initialization.");
            return;
        }

        log.info("Loading demo data...");
        seedDevices();
        seedWeatherData();
        seedAnimalDetections();
        seedViewerSession();
        log.info("Demo data ready: 2 devices, 11 weather readings, 6 animal detections, 1 viewer session.");
    }

    // ── Devices ──────────────────────────────────────────────────────────────

    private void seedDevices() {
        Device primary = new Device();
        primary.setId(DEVICE_1);
        primary.setName("Garden Camera Alpha");
        primary.setOwnerId(OWNER_ID);
        primary.setActiveServices(Set.of("animal_detector", "weather_station"));
        primary.setLatitude(48.2485);
        primary.setLongitude(11.6525);
        primary.setIsPrimary(true);
        primary.setLastSeen(LocalDateTime.now().minusMinutes(3));
        primary.setIpAddress("192.168.1.101");
        primary.setStreamActive(false);

        Device secondary = new Device();
        secondary.setId(DEVICE_2);
        secondary.setName("Forest Monitor Beta");
        secondary.setOwnerId(OWNER_ID);
        secondary.setActiveServices(Set.of("animal_detector"));
        secondary.setLatitude(48.0890);
        secondary.setLongitude(11.4724);
        secondary.setIsPrimary(false);
        secondary.setLastSeen(LocalDateTime.now().minusMinutes(15));
        secondary.setIpAddress("192.168.1.102");
        secondary.setStreamActive(false);

        deviceRepository.saveAll(List.of(primary, secondary));
        log.debug("Seeded 2 demo devices.");
    }

    // ── Weather readings ──────────────────────────────────────────────────────

    private void seedWeatherData() {
        LocalDateTime base = LocalDateTime.now().minusHours(1);

        List<RaspiWeatherData> readings = List.of(
            weather(DEVICE_1, LOCATION_1, base,                   21.3f, 58.2f, 1013.5f, 420f,  2.1f, 41200),
            weather(DEVICE_1, LOCATION_1, base.plusMinutes(6),    21.5f, 57.9f, 1013.4f, 480f,  2.3f, 40800),
            weather(DEVICE_1, LOCATION_1, base.plusMinutes(12),   21.8f, 57.5f, 1013.3f, 550f,  2.6f, 40500),
            weather(DEVICE_1, LOCATION_1, base.plusMinutes(18),   22.0f, 57.1f, 1013.1f, 610f,  2.8f, 40100),
            weather(DEVICE_1, LOCATION_1, base.plusMinutes(24),   22.3f, 56.8f, 1013.0f, 680f,  3.0f, 39800),
            weather(DEVICE_1, LOCATION_1, base.plusMinutes(30),   22.5f, 56.5f, 1012.8f, 720f,  3.1f, 39500),
            weather(DEVICE_1, LOCATION_1, base.plusMinutes(36),   22.4f, 56.7f, 1012.7f, 700f,  3.0f, 39600),
            weather(DEVICE_2, LOCATION_2, base,                   19.8f, 63.4f, 1014.2f, 210f,  1.2f, 44300),
            weather(DEVICE_2, LOCATION_2, base.plusMinutes(15),   20.1f, 62.9f, 1014.0f, 260f,  1.4f, 43900),
            weather(DEVICE_2, LOCATION_2, base.plusMinutes(30),   20.4f, 62.5f, 1013.8f, 310f,  1.6f, 43500),
            weather(DEVICE_2, LOCATION_2, base.plusMinutes(45),   20.6f, 62.2f, 1013.6f, 350f,  1.7f, 43200)
        );

        weatherDataRepository.saveAll(readings);
        log.debug("Seeded {} demo weather readings.", readings.size());
    }

    private RaspiWeatherData weather(String deviceId, String location, LocalDateTime ts,
                                     float temp, float humidity, float pressure,
                                     float lux, float uvi, int gasResistance) {
        RaspiWeatherData d = new RaspiWeatherData();
        d.setDeviceId(deviceId);
        d.setLocation(location);
        d.setTimestamp(ts);
        d.setTemperature(temp);
        d.setHumidity(humidity);
        d.setPressure(pressure);
        d.setLux(lux);
        d.setUvi(uvi);
        d.setGasResistance(gasResistance);
        return d;
    }

    // ── Animal detections ─────────────────────────────────────────────────────

    private void seedAnimalDetections() {
        LocalDateTime base = LocalDateTime.now().minusHours(2);

        List<AnimalDetection> detections = List.of(
            detection(DEVICE_1, LOCATION_1, "Turdus merula",        "bird",   0.918f, base),
            detection(DEVICE_1, LOCATION_1, "Erithacus rubecula",   "bird",   0.873f, base.plusMinutes(22)),
            detection(DEVICE_1, LOCATION_1, "Parus major",          "bird",   0.934f, base.plusMinutes(47)),
            detection(DEVICE_2, LOCATION_2, "Columba palumbus",     "bird",   0.851f, base.plusMinutes(30)),
            detection(DEVICE_2, LOCATION_2, "Vulpes vulpes",        "mammal", 0.824f, base.plusMinutes(65)),
            detection(DEVICE_2, LOCATION_2, "Capreolus capreolus",  "mammal", 0.783f, base.plusMinutes(90))
        );

        animalDetectionRepository.saveAll(detections);
        log.debug("Seeded {} demo animal detections.", detections.size());
    }

    private AnimalDetection detection(String deviceId, String location,
                                      String species, String animalType,
                                      float confidence, LocalDateTime ts) {
        AnimalDetection d = new AnimalDetection();
        d.setDeviceId(deviceId);
        d.setLocation(location);
        d.setSpecies(species);
        d.setAnimalType(animalType);
        d.setConfidence(confidence);
        d.setTimestamp(ts);
        d.setProcessedAt(ts.plusSeconds(2));
        d.setAiClassifiedAt(ts.plusSeconds(3));
        d.setAiProcessed(true);
        d.setX(0.30f);
        d.setY(0.25f);
        d.setWidth(0.15f);
        d.setHeight(0.20f);
        d.setTriggerReason("motion");
        return d;
    }

    // ── Viewer session ────────────────────────────────────────────────────────

    private void seedViewerSession() {
        try {
            viewerSessionService.joinStream(DEVICE_1, "demo-viewer-session-001");
            log.debug("Demo viewer session created for device {}.", DEVICE_1);
        } catch (Exception e) {
            log.warn("Could not create demo viewer session (Redis unavailable?): {}", e.getMessage());
        }
    }
}
