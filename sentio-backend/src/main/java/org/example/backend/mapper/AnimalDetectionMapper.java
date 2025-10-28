package org.example.backend.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.backend.dto.AnimalDetectionDTO;
import org.example.backend.model.AnimalDetection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for unified animal detection structure.
 * Handles conversion between AnimalDetection entities and DTOs.
 */
@Component
public class AnimalDetectionMapper {

    private static final Logger logger = LoggerFactory.getLogger(AnimalDetectionMapper.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnimalDetectionDTO toDTO(AnimalDetection entity) {
        if (entity == null) {
            return null;
        }

        AnimalDetectionDTO dto = new AnimalDetectionDTO();
        dto.setId(entity.getId());
        dto.setSpecies(entity.getSpecies());
        dto.setAnimalType(entity.getAnimalType());
        dto.setConfidence(entity.getConfidence());
        dto.setOriginalSpecies(entity.getOriginalSpecies());
        dto.setOriginalConfidence(entity.getOriginalConfidence());

        // Parse alternate species from JSON
        if (entity.getAlternateSpecies() != null && !entity.getAlternateSpecies().isEmpty()) {
            try {
                List<AnimalDetectionDTO.AlternateSpeciesDTO> alternateSpecies =
                        objectMapper.readValue(entity.getAlternateSpecies(),
                                new TypeReference<>() {
                                });
                dto.setAlternateSpecies(alternateSpecies);
            } catch (JsonProcessingException e) {
                logger.warn("Error parsing alternate species JSON for detection ID {}: {}",
                        entity.getId(), e.getMessage());
                dto.setAlternateSpecies(new ArrayList<>());
            }
        }

        // Set all other fields
        dto.setX(entity.getX());
        dto.setY(entity.getY());
        dto.setWidth(entity.getWidth());
        dto.setHeight(entity.getHeight());
        dto.setClassId(entity.getClassId());
        dto.setImageUrl(entity.getImageUrl());
        dto.setTimestamp(entity.getTimestamp());
        dto.setDeviceId(entity.getDeviceId());
        dto.setLocation(entity.getLocation());
        dto.setTriggerReason(entity.getTriggerReason());
        dto.setProcessedAt(entity.getProcessedAt());
        dto.setAiClassifiedAt(entity.getAiClassifiedAt());
        dto.setAiProcessed(entity.isAiProcessed());

        return dto;
    }

    public List<AnimalDetectionDTO> toDTOList(List<AnimalDetection> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public AnimalDetection toEntity(AnimalDetectionDTO dto) {
        if (dto == null) {
            return null;
        }

        AnimalDetection entity = new AnimalDetection();
        entity.setId(dto.getId());
        entity.setSpecies(dto.getSpecies());
        entity.setAnimalType(dto.getAnimalType());
        entity.setConfidence(dto.getConfidence());
        entity.setOriginalSpecies(dto.getOriginalSpecies());
        entity.setOriginalConfidence(dto.getOriginalConfidence());

        // Convert alternate species to JSON
        if (dto.getAlternateSpecies() != null && !dto.getAlternateSpecies().isEmpty()) {
            try {
                String alternateSpeciesJson = objectMapper.writeValueAsString(dto.getAlternateSpecies());
                entity.setAlternateSpecies(alternateSpeciesJson);
            } catch (JsonProcessingException e) {
                logger.warn("Error serializing alternate species to JSON: {}", e.getMessage());
            }
        }

        // Set all other fields
        entity.setX(dto.getX());
        entity.setY(dto.getY());
        entity.setWidth(dto.getWidth());
        entity.setHeight(dto.getHeight());
        entity.setClassId(dto.getClassId());
        entity.setImageUrl(dto.getImageUrl());
        entity.setTimestamp(dto.getTimestamp());
        entity.setDeviceId(dto.getDeviceId());
        entity.setLocation(dto.getLocation());
        entity.setTriggerReason(dto.getTriggerReason());
        entity.setProcessedAt(dto.getProcessedAt());
        entity.setAiClassifiedAt(dto.getAiClassifiedAt());
        entity.setAiProcessed(dto.isAiProcessed());

        return entity;
    }
}