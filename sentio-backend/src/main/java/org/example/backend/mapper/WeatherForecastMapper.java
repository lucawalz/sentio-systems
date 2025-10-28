package org.example.backend.mapper;

import lombok.extern.slf4j.Slf4j;
import org.example.backend.dto.WeatherForecastDTO;
import org.example.backend.model.WeatherForecast;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper component responsible for converting between WeatherForecast entities and DTOs.
 * Updated for Open-Meteo hourly forecast data structure.
 */
@Component
@Slf4j
public class WeatherForecastMapper {

    /**
     * Converts a WeatherForecast entity to a WeatherForecastDTO.
     */
    public WeatherForecastDTO toDTO(WeatherForecast entity) {
        if (entity == null) {
            log.debug("Null WeatherForecast entity provided for DTO conversion");
            return null;
        }

        log.debug("Converting WeatherForecast entity to DTO - ID: {}, Location: {}, {}",
                entity.getId(), entity.getCity(), entity.getCountry());

        WeatherForecastDTO dto = new WeatherForecastDTO();
        dto.setId(entity.getId());
        dto.setForecastDate(entity.getForecastDate());
        dto.setForecastDateTime(entity.getForecastDateTime());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        // Core temperature and atmospheric data
        dto.setTemperature(entity.getTemperature());
        dto.setHumidity(entity.getHumidity());
        dto.setApparentTemperature(entity.getApparentTemperature());
        dto.setPressure(entity.getPressure());
        dto.setDescription(entity.getDescription());
        dto.setWeatherMain(entity.getWeatherMain());
        dto.setIcon(entity.getIcon());

        // Wind data
        dto.setWindSpeed(entity.getWindSpeed());
        dto.setWindDirection(entity.getWindDirection());
        dto.setWindGusts(entity.getWindGusts());
        dto.setCloudCover(entity.getCloudCover());
        dto.setVisibility(entity.getVisibility());

        // Precipitation data
        dto.setPrecipitation(entity.getPrecipitation());
        dto.setRain(entity.getRain());
        dto.setShowers(entity.getShowers());
        dto.setSnowfall(entity.getSnowfall());
        dto.setSnowDepth(entity.getSnowDepth());
        dto.setDewPoint(entity.getDewPoint());
        dto.setPrecipitationProbability(entity.getPrecipitationProbability());
        dto.setWeatherCode(entity.getWeatherCode());

        // Location data
        dto.setCity(entity.getCity());
        dto.setCountry(entity.getCountry());
        dto.setLatitude(entity.getLatitude());
        dto.setLongitude(entity.getLongitude());
        dto.setDetectedLocation(entity.getDetectedLocation());

        return dto;
    }

    /**
     * Converts a list of WeatherForecast entities to WeatherForecastDTO list.
     */
    public List<WeatherForecastDTO> toDTOList(List<WeatherForecast> entities) {
        if (entities == null) {
            log.debug("Null entities list provided for DTO list conversion");
            return null;
        }

        log.debug("Converting {} WeatherForecast entities to DTO list", entities.size());

        return entities.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}