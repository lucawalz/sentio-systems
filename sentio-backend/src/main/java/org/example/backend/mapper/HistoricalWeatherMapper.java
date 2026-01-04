package org.example.backend.mapper;

import lombok.extern.slf4j.Slf4j;
import org.example.backend.dto.HistoricalWeatherDTO;
import org.example.backend.model.HistoricalWeather;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper component responsible for converting between HistoricalWeather
 * entities and DTOs.
 * Provides bidirectional mapping functionality for historical weather data
 * transfer objects.
 * <p>
 * This mapper handles the conversion of historical weather data between the
 * persistence
 * layer (entities) and the presentation layer (DTOs). It includes null-safe
 * operations
 * and supports both individual object mapping and bulk list transformations for
 * efficient data transfer in REST API operations.
 * </p>
 *
 * @author Sentio Team
 * @version 1.0
 * @since 1.0
 */
@Component
@Slf4j
public class HistoricalWeatherMapper {

    /**
     * Converts a HistoricalWeather entity to a HistoricalWeatherDTO.
     *
     * @param entity The historical weather entity to convert
     * @return HistoricalWeatherDTO or null if input entity is null
     */
    public HistoricalWeatherDTO toDTO(HistoricalWeather entity) {
        if (entity == null) {
            log.debug("Null HistoricalWeather entity provided for DTO conversion");
            return null;
        }

        log.debug("Converting HistoricalWeather entity to DTO - ID: {}, Location: {}, {}, Date: {}",
                entity.getId(), entity.getCity(), entity.getCountry(), entity.getWeatherDate());

        HistoricalWeatherDTO dto = new HistoricalWeatherDTO();
        dto.setId(entity.getId());
        dto.setWeatherDate(entity.getWeatherDate());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setWeatherCode(entity.getWeatherCode());
        dto.setMaxTemperature(entity.getMaxTemperature());
        dto.setMinTemperature(entity.getMinTemperature());

        // Calculate mean temperature from max and min for comparison charts
        if (entity.getMaxTemperature() != null && entity.getMinTemperature() != null) {
            dto.setTemperatureMean((entity.getMaxTemperature() + entity.getMinTemperature()) / 2.0f);
        }

        dto.setSunrise(entity.getSunrise());
        dto.setSunset(entity.getSunset());
        dto.setDaylightDuration(entity.getDaylightDuration());
        dto.setSunshineDuration(entity.getSunshineDuration());
        dto.setUvIndexMax(entity.getUvIndexMax());
        dto.setPrecipitationSum(entity.getPrecipitationSum());
        dto.setPrecipitationHours(entity.getPrecipitationHours());
        dto.setWindSpeedMax(entity.getWindSpeedMax());
        dto.setWindDirectionDominant(entity.getWindDirectionDominant());
        dto.setDescription(entity.getDescription());
        dto.setWeatherMain(entity.getWeatherMain());
        dto.setIcon(entity.getIcon());
        dto.setCity(entity.getCity());
        dto.setCountry(entity.getCountry());
        dto.setLatitude(entity.getLatitude());
        dto.setLongitude(entity.getLongitude());
        dto.setDetectedLocation(entity.getDetectedLocation());

        return dto;
    }

    /**
     * Converts a list of HistoricalWeather entities to HistoricalWeatherDTO list.
     *
     * @param entities List of historical weather entities to convert
     * @return List of HistoricalWeatherDTOs
     */
    public List<HistoricalWeatherDTO> toDTOList(List<HistoricalWeather> entities) {
        if (entities == null) {
            log.debug("Null entities list provided for DTO list conversion");
            return null;
        }

        log.debug("Converting {} HistoricalWeather entities to DTO list", entities.size());

        return entities.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}