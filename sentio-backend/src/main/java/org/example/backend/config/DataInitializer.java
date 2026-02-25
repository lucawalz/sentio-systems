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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.example.backend.event.DeviceLocationUpdatedEvent;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import jakarta.ws.rs.core.Response;

/**
 * Seeds realistic demo data when the application starts with the {@code demo}
 * profile.
 *
 * <p>
 * Activate via environment variable or {@code .env}:
 * 
 * <pre>
 * SPRING_PROFILES_ACTIVE = demo
 * </pre>
 *
 * <p>
 * Data is only seeded once. If devices already exist in the database the
 * initializer skips all inserts. This prevents duplicate data on restarts.
 *
 * <p>
 * Demo data does <strong>not</strong> load in the {@code prod} profile.
 */
@Component
@Profile("demo")
@Slf4j
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final String DEVICE_1 = "demo-device-001";
    private static final String DEVICE_2 = "demo-device-002";
    private static final String DEMO_USERNAME = "demo";
    private static final String LOCATION_1 = "Garching, Bayern";
    private static final String LOCATION_2 = "Forstenried, München";

    private final DeviceRepository deviceRepository;
    private final RaspiWeatherDataRepository weatherDataRepository;
    private final AnimalDetectionRepository animalDetectionRepository;
    private final ViewerSessionService viewerSessionService;
    private final Keycloak keycloak;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${keycloak.realm}")
    private String realm;

    @Override
    public void run(ApplicationArguments args) {
        if (deviceRepository.count() > 0) {
            log.info("Demo data already present — skipping initialization.");
            return;
        }

        log.info("Loading demo data...");
        String ownerId = seedKeycloakUser();
        seedDevices(ownerId);
        seedWeatherData();
        seedAnimalDetections();
        seedViewerSession();
        log.info("Demo data ready: 2 devices, 7-day sensor/animal data, 1 viewer session. Weather fetch triggered.");
    }

    // ── Keycloak User ────────────────────────────────────────────────────────

    private String seedKeycloakUser() {
        UsersResource usersResource = keycloak.realm(realm).users();

        // Skip if already exists
        List<UserRepresentation> existing = usersResource.search(DEMO_USERNAME, true);
        if (!existing.isEmpty()) {
            log.info("Demo user already exists in Keycloak.");
            return existing.get(0).getId();
        }

        UserRepresentation user = new UserRepresentation();
        user.setUsername(DEMO_USERNAME);
        user.setEmail("demo@syslabs.dev");
        user.setFirstName("Demo");
        user.setLastName("Admin");
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setRealmRoles(List.of("admin", "user"));

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue("Demo@123!");
        credential.setTemporary(false);
        user.setCredentials(Collections.singletonList(credential));

        Response response = usersResource.create(user);
        if (response.getStatus() != 201) {
            log.error("Failed to seed demo user in Keycloak. Status: {}", response.getStatus());
            throw new RuntimeException("Keycloak demo user creation failed.");
        }

        String userId = response.getLocation().getPath().replaceAll(".*/(.*)$", "$1");
        log.info("Successfully seeded demo user in Keycloak with ID: {}", userId);
        return userId;
    }

    // ── Devices ──────────────────────────────────────────────────────────────

    private void seedDevices(String ownerId) {
        Device primary = new Device();
        primary.setId(DEVICE_1);
        primary.setName("Garden Camera Alpha");
        primary.setOwnerId(ownerId);
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
        secondary.setOwnerId(ownerId);
        secondary.setActiveServices(Set.of("animal_detector"));
        secondary.setLatitude(48.0890);
        secondary.setLongitude(11.4724);
        secondary.setIsPrimary(false);
        secondary.setLastSeen(LocalDateTime.now().minusMinutes(15));
        secondary.setIpAddress("192.168.1.102");
        secondary.setStreamActive(false);

        deviceRepository.saveAll(List.of(primary, secondary));
        log.debug("Seeded 2 demo devices.");

        // Trigger Event-Driven Architecture (EDA) to fetch real historical weather,
        // forecasts, and alerts
        log.info("Publishing DeviceLocationUpdatedEvent to trigger EDA weather data fetch...");
        eventPublisher.publishEvent(
                new DeviceLocationUpdatedEvent(this, DEVICE_1, primary.getLatitude(), primary.getLongitude(), true));
        eventPublisher.publishEvent(new DeviceLocationUpdatedEvent(this, DEVICE_2, secondary.getLatitude(),
                secondary.getLongitude(), true));
    }

    // ── Weather readings (Sensor Telemetry) ───────────────────────────────────

    private void seedWeatherData() {
        log.info("Seeding 7 days of historical demo sensor (RaspiWeatherData) readings...");
        LocalDateTime now = LocalDateTime.now();
        List<RaspiWeatherData> readings = new java.util.ArrayList<>();
        java.util.Random random = new java.util.Random();

        for (int day = 0; day <= 6; day++) {
            for (int hour = 0; hour < 24; hour++) {
                LocalDateTime ts = now.minusDays(day).minusHours(23 - hour).withMinute(0).withSecond(0).withNano(0);

                // DEVICE 1: HdM Stuttgart (COLD winter: -6 to 2°C)
                float baseTemp1 = -4.0f;
                if (hour >= 0 && hour < 6)
                    baseTemp1 = -5.0f;
                else if (hour >= 6 && hour < 10)
                    baseTemp1 = -3.0f;
                else if (hour >= 10 && hour < 15)
                    baseTemp1 = 0.0f;
                else if (hour >= 15 && hour < 18)
                    baseTemp1 = -2.0f;

                float temp1 = baseTemp1 + (random.nextFloat() * 3 - 1.5f);
                float humidity1 = 75 + random.nextInt(15);
                float pressure1 = 1025 + random.nextInt(10);
                float lux1 = 20 + ((hour >= 8 && hour <= 17) ? hour * 40 : 10) + random.nextInt(50);
                float uvi1 = (hour >= 10 && hour <= 15) ? (random.nextFloat() * 1.5f) : 0.0f;

                readings.add(weather(DEVICE_1, LOCATION_1, ts, temp1, humidity1, pressure1, lux1, uvi1,
                        40000 + random.nextInt(5000)));

                // DEVICE 2: Hamburg (VERY COLD winter: -8 to 0°C)
                float baseTemp2 = -6.0f;
                if (hour >= 0 && hour < 7)
                    baseTemp2 = -7.0f;
                else if (hour >= 7 && hour < 11)
                    baseTemp2 = -5.0f;
                else if (hour >= 11 && hour < 14)
                    baseTemp2 = -2.0f;
                else if (hour >= 14 && hour < 17)
                    baseTemp2 = -3.0f;

                float temp2 = baseTemp2 + (random.nextFloat() * 3 - 1.5f);
                float humidity2 = 80 + random.nextInt(15);
                float pressure2 = 1028 + random.nextInt(8);
                float lux2 = 15 + ((hour >= 9 && hour <= 16) ? hour * 25 : 5) + random.nextInt(30);
                float uvi2 = (hour >= 11 && hour <= 14) ? (random.nextFloat() * 1.0f) : 0.0f;

                readings.add(weather(DEVICE_2, LOCATION_2, ts, temp2, humidity2, pressure2, lux2, uvi2,
                        42000 + random.nextInt(4000)));
            }
        }

        weatherDataRepository.saveAll(readings);
        log.debug("Seeded {} demo sensor readings.", readings.size());
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
        log.info("Seeding 7 days of historical demo animal detections...");
        LocalDateTime now = LocalDateTime.now();
        List<AnimalDetection> detections = new java.util.ArrayList<>();
        java.util.Random random = new java.util.Random();

        String[] birdSpecies = { "Robin", "Sparrow", "Blue Tit", "Blackbird", "Magpie", "Great Tit", "Wren", "Starling",
                "Chaffinch", "Goldfinch" };
        String[] mammalSpecies = { "Fox", "Squirrel", "Hedgehog", "Rabbit", "Deer", "Badger", "Mouse", "Rat", "Mole",
                "Stoat" };

        // Generating 7 days of data for both devices
        for (int day = 0; day <= 6; day++) {
            LocalDateTime dayBase = now.minusDays(day);

            // DEVICE 1: Weather Station -> MAMMALS
            int numMammals = 3 + random.nextInt(4); // 3 to 6 detections per day
            for (int i = 0; i < numMammals; i++) {
                // Mammals more active at dawn/dusk
                int hour = random.nextBoolean() ? (5 + random.nextInt(3)) : (18 + random.nextInt(4));
                LocalDateTime ts = dayBase.withHour(hour).withMinute(random.nextInt(60)).withSecond(random.nextInt(60));

                String species = mammalSpecies[random.nextInt(mammalSpecies.length)];
                float conf = 0.60f + (random.nextFloat() * 0.35f);

                AnimalDetection d = detection(DEVICE_1, LOCATION_1, species, "mammal", conf, ts);
                detections.add(d);
            }

            // DEVICE 2: Garden Camera -> BIRDS
            int numBirds = 8 + random.nextInt(5); // 8 to 12 detections per day
            for (int i = 0; i < numBirds; i++) {
                // Birds more active in morning
                int hour = (random.nextInt(3) < 2) ? (6 + random.nextInt(14)) : random.nextInt(24);
                LocalDateTime ts = dayBase.withHour(hour).withMinute(random.nextInt(60)).withSecond(random.nextInt(60));

                String species = birdSpecies[random.nextInt(birdSpecies.length)];
                float conf = 0.75f + (random.nextFloat() * 0.24f);

                AnimalDetection d = detection(DEVICE_2, LOCATION_2, species, "bird", conf, ts);
                detections.add(d);
            }
        }

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
        d.setX(0.10f + (new java.util.Random().nextFloat() * 0.60f));
        d.setY(0.10f + (new java.util.Random().nextFloat() * 0.60f));
        d.setWidth(0.10f + (new java.util.Random().nextFloat() * 0.20f));
        d.setHeight(0.10f + (new java.util.Random().nextFloat() * 0.20f));
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
