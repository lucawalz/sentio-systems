package org.example.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.example.backend.model.*;
import org.example.backend.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Demo data seeder for development and testing environments.
 * Seeds realistic data across all entities when the 'dev' profile is active.
 * 
 * Activate with: --spring.profiles.active=dev
 */
@Slf4j
@Configuration
@Profile("dev")
public class DemoDataSeeder {

    @Bean
    @ConditionalOnProperty(name = "demo.data.enabled", havingValue = "true", matchIfMissing = true)
    CommandLineRunner seedDemoData(
            DeviceRepository deviceRepository,
            LocationDataRepository locationRepository,
            AnimalDetectionRepository animalDetectionRepository,
            WeatherForecastRepository forecastRepository,
            HistoricalWeatherRepository historicalRepository,
            WeatherAlertRepository alertRepository,
            RaspiWeatherDataRepository raspiWeatherRepository) {
        return args -> {
            log.info("🌱 Starting demo data seeding...");

            // Clear existing demo data
            animalDetectionRepository.deleteAll();
            raspiWeatherRepository.deleteAll();
            forecastRepository.deleteAll();
            historicalRepository.deleteAll();
            alertRepository.deleteAll();
            locationRepository.deleteAll();

            // Seed devices
            List<Device> devices = seedDevices(deviceRepository);
            log.info("✅ Seeded {} devices", devices.size());

            // Seed locations
            List<LocationData> locations = seedLocations(locationRepository, devices);
            log.info("✅ Seeded {} locations", locations.size());

            // Seed weather forecasts
            int forecasts = seedWeatherForecasts(forecastRepository, devices);
            log.info("✅ Seeded {} weather forecasts", forecasts);

            // Seed historical weather
            int historical = seedHistoricalWeather(historicalRepository, devices);
            log.info("✅ Seeded {} historical weather records", historical);

            // Seed animal detections
            int detections = seedAnimalDetections(animalDetectionRepository, devices);
            log.info("✅ Seeded {} animal detections", detections);

            // Seed weather alerts
            int alerts = seedWeatherAlerts(alertRepository, devices);
            log.info("✅ Seeded {} weather alerts", alerts);

            // Seed Raspi weather data
            int raspiData = seedRaspiWeatherData(raspiWeatherRepository, devices);
            log.info("✅ Seeded {} Raspi weather data records", raspiData);

            log.info("🌱 Demo data seeding completed successfully!");
        };
    }

    private List<Device> seedDevices(DeviceRepository repository) {
        Device device1 = new Device();
        device1.setId("device-001");
        device1.setName("Garden Camera");
        device1.setOwnerId("owner-demo-1");
        device1.setLatitude(52.5200);
        device1.setLongitude(13.4050);
        device1.setIpAddress("192.168.1.100");
        device1.setIsPrimary(true);
        device1.setLastSeen(LocalDateTime.now());
        device1.setStreamActive(false);

        Device device2 = new Device();
        device2.setId("device-002");
        device2.setName("Backyard Monitor");
        device2.setOwnerId("owner-demo-1");
        device2.setLatitude(48.1351);
        device2.setLongitude(11.5820);
        device2.setIpAddress("192.168.1.101");
        device2.setIsPrimary(false);
        device2.setLastSeen(LocalDateTime.now().minusHours(2));

        Device device3 = new Device();
        device3.setId("device-003");
        device3.setName("Forest Watcher");
        device3.setOwnerId("owner-demo-2");
        device3.setLatitude(50.1109);
        device3.setLongitude(8.6821);
        device3.setIpAddress("192.168.1.102");
        device3.setIsPrimary(true);
        device3.setLastSeen(LocalDateTime.now().minusMinutes(30));

        return repository.saveAll(Arrays.asList(device1, device2, device3));
    }

    private List<LocationData> seedLocations(LocationDataRepository repository, List<Device> devices) {
        LocationData loc1 = createLocation("Berlin", "Germany", 52.52f, 13.41f, devices.get(0).getId());
        LocationData loc2 = createLocation("Munich", "Germany", 48.14f, 11.58f, devices.get(1).getId());
        LocationData loc3 = createLocation("Frankfurt", "Germany", 50.11f, 8.68f, devices.get(2).getId());

        return repository.saveAll(Arrays.asList(loc1, loc2, loc3));
    }

    private LocationData createLocation(String city, String country, float lat, float lon, String deviceId) {
        LocationData location = new LocationData();
        location.setCity(city);
        location.setCountry(country);
        location.setRegion(city + " Region");
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setIpAddress("192.168.1.1");
        location.setTimezone("Europe/Berlin");
        location.setDeviceId(deviceId);
        return location;
    }

    private int seedWeatherForecasts(WeatherForecastRepository repository, List<Device> devices) {
        int count = 0;
        LocalDate today = LocalDate.now();

        for (Device device : devices) {
            // Create 7 days of forecasts, 4 times per day
            for (int day = 0; day < 7; day++) {
                LocalDate forecastDate = today.plusDays(day);
                for (int hour : Arrays.asList(0, 6, 12, 18)) {
                    LocalDateTime forecastDateTime = forecastDate.atTime(hour, 0);

                    WeatherForecast forecast = new WeatherForecast();
                    forecast.setDeviceId(device.getId());
                    forecast.setForecastDate(forecastDate);
                    forecast.setForecastDateTime(forecastDateTime);
                    forecast.setTemperature(15f + (float) (Math.random() * 15));
                    forecast.setHumidity(50f + (float) (Math.random() * 40));
                    forecast.setPressure(1000f + (float) (Math.random() * 40));
                    forecast.setWindSpeed(5f + (float) (Math.random() * 15));
                    forecast.setLatitude(device.getLatitude().floatValue());
                    forecast.setLongitude(device.getLongitude().floatValue());

                    repository.save(forecast);
                    count++;
                }
            }
        }

        return count;
    }

    private int seedHistoricalWeather(HistoricalWeatherRepository repository, List<Device> devices) {
        int count = 0;
        LocalDate today = LocalDate.now();

        for (Device device : devices) {
            // Create 30 days of historical data
            for (int i = 1; i <= 30; i++) {
                LocalDate weatherDate = today.minusDays(i);

                HistoricalWeather historical = new HistoricalWeather();
                historical.setDeviceId(device.getId());
                historical.setWeatherDate(weatherDate);
                historical.setMaxTemperature(20f + (float) (Math.random() * 10));
                historical.setMinTemperature(10f + (float) (Math.random() * 10));
                historical.setPrecipitationSum((float) (Math.random() * 10));
                historical.setWindSpeedMax(10f + (float) (Math.random() * 20));
                historical.setLatitude(device.getLatitude().floatValue());
                historical.setLongitude(device.getLongitude().floatValue());

                repository.save(historical);
                count++;
            }
        }

        return count;
    }

    private int seedAnimalDetections(AnimalDetectionRepository repository, List<Device> devices) {
        int count = 0;
        LocalDateTime now = LocalDateTime.now();

        String[] birdSpecies = { "Sparrow", "Robin", "Blue Tit", "Great Tit", "Blackbird", "Starling" };
        String[] mammalSpecies = { "Deer", "Fox", "Squirrel", "Hedgehog" };

        for (Device device : devices) {
            // Create bird detections
            for (int i = 0; i < 7; i++) {
                String species = birdSpecies[(int) (Math.random() * birdSpecies.length)];
                AnimalDetection detection = createDetection(
                        species, "bird",
                        0.75f + (float) (Math.random() * 0.25),
                        now.minusHours(i * 3),
                        device.getId());
                repository.save(detection);
                count++;
            }

            // Create mammal detections
            for (int i = 0; i < 3; i++) {
                String species = mammalSpecies[(int) (Math.random() * mammalSpecies.length)];
                AnimalDetection detection = createDetection(
                        species, "mammal",
                        0.70f + (float) (Math.random() * 0.30),
                        now.minusDays(i),
                        device.getId());
                repository.save(detection);
                count++;
            }
        }

        return count;
    }

    private AnimalDetection createDetection(String species, String animalType, float confidence,
            LocalDateTime timestamp, String deviceId) {
        AnimalDetection detection = new AnimalDetection();
        detection.setSpecies(species);
        detection.setAnimalType(animalType);
        detection.setConfidence(confidence);
        detection.setX(100f + (float) (Math.random() * 500));
        detection.setY(100f + (float) (Math.random() * 500));
        detection.setWidth(50f + (float) (Math.random() * 100));
        detection.setHeight(50f + (float) (Math.random() * 100));
        detection.setTimestamp(timestamp);
        detection.setDeviceId(deviceId);
        detection
                .setImageUrl("/images/detections/" + species.toLowerCase() + "_" + System.currentTimeMillis() + ".jpg");
        detection.setLocation("Demo Location");
        return detection;
    }

    private int seedWeatherAlerts(WeatherAlertRepository repository, List<Device> devices) {
        int count = 0;
        LocalDateTime now = LocalDateTime.now();

        String[] severities = { "minor", "moderate", "severe" };
        String[] eventTypes = { "Thunderstorm", "Heavy Rain", "Strong Wind", "Fog" };

        for (int i = 0; i < devices.size(); i++) {
            Device device = devices.get(i);
            String eventType = eventTypes[i % eventTypes.length];
            String severity = severities[i % severities.length];

            WeatherAlert alert = new WeatherAlert();
            alert.setDeviceId(device.getId());
            alert.setAlertId("DEMO-ALERT-" + (1000 + count));
            alert.setStatus("actual");
            alert.setEffective(now.minusHours(1));
            alert.setExpires(now.plusHours(6));
            alert.setSeverity(severity);
            alert.setUrgency("immediate");
            alert.setCertainty("likely");
            alert.setEventEn(eventType + " Warning");
            alert.setHeadlineEn(eventType + " expected in your area");
            alert.setDescriptionEn("A " + severity + " " + eventType.toLowerCase() + " is expected.");
            alert.setLatitude(device.getLatitude().floatValue());
            alert.setLongitude(device.getLongitude().floatValue());

            repository.save(alert);
            count++;
        }

        return count;
    }

    private int seedRaspiWeatherData(RaspiWeatherDataRepository repository, List<Device> devices) {
        int count = 0;
        LocalDateTime now = LocalDateTime.now();

        for (Device device : devices) {
            // Create 24 hours of data (one per hour)
            for (int i = 0; i < 24; i++) {
                RaspiWeatherData data = new RaspiWeatherData();
                data.setDeviceId(device.getId());
                data.setLocation("Demo Location");
                data.setTemperature(15f + (float) (Math.random() * 15));
                data.setHumidity(40f + (float) (Math.random() * 40));
                data.setPressure(990f + (float) (Math.random() * 40));
                data.setLux(100f + (float) (Math.random() * 900));
                data.setUvi(0f + (float) (Math.random() * 10));
                data.setTimestamp(now.minusHours(i));

                repository.save(data);
                count++;
            }
        }

        return count;
    }
}
