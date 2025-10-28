package org.example.backend.mapper;

import org.example.backend.dto.LocationDataDTO;
import org.example.backend.model.LocationData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


/**
 * Mapper component for converting between LocationData entities and DTOs.
 * Handles bidirectional mapping and null-safe operations.
 */
@Component
public class LocationDataMapper {

    private static final Logger logger = LoggerFactory.getLogger(LocationDataMapper.class);

    /**
     * Converts a LocationData entity to its corresponding DTO.
     * @param entity the LocationData entity to convert
     * @return LocationDataDTO or null if input is null
     */
    public LocationDataDTO toDTO(LocationData entity) {
        if (entity == null) {
            logger.debug("Received null LocationData entity for DTO conversion");
            return null;
        }

        logger.debug("Converting LocationData entity with ID {} to DTO", entity.getId());

        LocationDataDTO dto = new LocationDataDTO();
        dto.setId(entity.getId());
        dto.setIpAddress(entity.getIpAddress());
        dto.setCity(entity.getCity());
        dto.setCountry(entity.getCountry());
        dto.setRegion(entity.getRegion());
        dto.setLatitude(entity.getLatitude());
        dto.setLongitude(entity.getLongitude());
        dto.setTimezone(entity.getTimezone());
        dto.setIsp(entity.getIsp());
        dto.setOrganization(entity.getOrganization());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        return dto;
    }
}