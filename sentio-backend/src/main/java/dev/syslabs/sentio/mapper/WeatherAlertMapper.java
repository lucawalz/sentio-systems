
package dev.syslabs.sentio.mapper;

import lombok.extern.slf4j.Slf4j;
import dev.syslabs.sentio.dto.WeatherAlertDTO;
import dev.syslabs.sentio.model.WeatherAlert;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper component responsible for converting between WeatherAlert entities and DTOs.
 * Provides bidirectional mapping functionality for weather alert data transfer objects.
 * <p>
 * This mapper handles the conversion of weather alert data between the persistence
 * layer (entities) and the presentation layer (DTOs). It includes null-safe operations,
 * localization support, and supports both individual object mapping and bulk list
 * transformations for efficient data transfer in REST API operations.
 * </p>
 *
 * @author Sentio Team
 * @version 1.0
 * @since 1.0
 */
@Component
@Slf4j
public class WeatherAlertMapper {

    /**
     * Converts a WeatherAlert entity to a WeatherAlertDTO.
     *
     * @param entity The weather alert entity to convert
     * @param preferGerman Whether to prefer German localization
     * @return WeatherAlertDTO or null if input entity is null
     */
    public WeatherAlertDTO toDTO(WeatherAlert entity, boolean preferGerman) {
        if (entity == null) {
            log.debug("Null WeatherAlert entity provided for DTO conversion");
            return null;
        }

        log.debug("Converting WeatherAlert entity to DTO - ID: {}, AlertId: {}, Severity: {}",
                entity.getId(), entity.getAlertId(), entity.getSeverity());

        WeatherAlertDTO dto = new WeatherAlertDTO();
        dto.setId(entity.getId());
        dto.setBrightSkyId(entity.getBrightSkyId());
        dto.setAlertId(entity.getAlertId());
        dto.setStatus(entity.getStatus());
        dto.setEffective(entity.getEffective());
        dto.setOnset(entity.getOnset());
        dto.setExpires(entity.getExpires());
        dto.setCategory(entity.getCategory());
        dto.setResponseType(entity.getResponseType());
        dto.setUrgency(entity.getUrgency());
        dto.setSeverity(entity.getSeverity());
        dto.setCertainty(entity.getCertainty());
        dto.setEventCode(entity.getEventCode());
        dto.setEventEn(entity.getEventEn());
        dto.setEventDe(entity.getEventDe());
        dto.setHeadlineEn(entity.getHeadlineEn());
        dto.setHeadlineDe(entity.getHeadlineDe());
        dto.setDescriptionEn(entity.getDescriptionEn());
        dto.setDescriptionDe(entity.getDescriptionDe());
        dto.setInstructionEn(entity.getInstructionEn());
        dto.setInstructionDe(entity.getInstructionDe());
        dto.setWarnCellId(entity.getWarnCellId());
        dto.setName(entity.getName());
        dto.setNameShort(entity.getNameShort());
        dto.setDistrict(entity.getDistrict());
        dto.setState(entity.getState());
        dto.setStateShort(entity.getStateShort());
        dto.setCity(entity.getCity());
        dto.setCountry(entity.getCountry());
        dto.setLatitude(entity.getLatitude());
        dto.setLongitude(entity.getLongitude());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setIsActive(entity.isActive());

        // Set localized fields based on preference
        dto.setLocalizedHeadline(entity.getLocalizedHeadline(preferGerman));
        dto.setLocalizedDescription(entity.getLocalizedDescription(preferGerman));
        dto.setLocalizedEvent(preferGerman && entity.getEventDe() != null ? entity.getEventDe() : entity.getEventEn());
        dto.setLocalizedInstruction(preferGerman && entity.getInstructionDe() != null ? entity.getInstructionDe() : entity.getInstructionEn());

        return dto;
    }

    /**
     * Converts a list of WeatherAlert entities to WeatherAlertDTO list.
     *
     * @param entities List of weather alert entities to convert
     * @param preferGerman Whether to prefer German localization
     * @return List of WeatherAlertDTOs
     */
    public List<WeatherAlertDTO> toDTOList(List<WeatherAlert> entities, boolean preferGerman) {
        if (entities == null) {
            log.debug("Null entities list provided for DTO list conversion");
            return null;
        }

        log.debug("Converting {} WeatherAlert entities to DTO list", entities.size());

        return entities.stream()
                .map(entity -> toDTO(entity, preferGerman))
                .collect(Collectors.toList());
    }
}