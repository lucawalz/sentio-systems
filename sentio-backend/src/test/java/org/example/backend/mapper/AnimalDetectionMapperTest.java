package org.example.backend.mapper;

import org.example.backend.dto.AnimalDetectionDTO;
import org.example.backend.model.AnimalDetection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnimalDetectionMapperTest {

    private AnimalDetectionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new AnimalDetectionMapper();
    }

    @Test
    void toDTO_shouldReturnNull_whenEntityIsNull() {
        assertNull(mapper.toDTO(null));
    }

    @Test
    void toEntity_shouldReturnNull_whenDtoIsNull() {
        assertNull(mapper.toEntity(null));
    }

    @Test
    void toDTO_shouldMapAllFields_andParseAlternateSpeciesJson() {
        // given
        AnimalDetection entity = new AnimalDetection();
        entity.setId(1L);
        entity.setSpecies("cat");
        entity.setAnimalType("mammal");
        entity.setConfidence(0.95f);
        entity.setOriginalSpecies("feline");
        entity.setOriginalConfidence(0.88f);

        entity.setAlternateSpecies("""
            [
              {"species":"dog","confidence":0.12},
              {"species":"fox","confidence":0.05}
            ]
            """);

        entity.setX(10.0f);
        entity.setY(20.0f);
        entity.setWidth(100.0f);
        entity.setHeight(200.0f);
        entity.setClassId(7);
        entity.setImageUrl("https://example.com/img.jpg");

        LocalDateTime ts = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        entity.setTimestamp(ts);

        entity.setDeviceId("dev-1");
        entity.setLocation("Berlin");
        entity.setTriggerReason("motion");

        LocalDateTime processedAt = LocalDateTime.of(2025, 1, 1, 10, 0, 10);
        entity.setProcessedAt(processedAt);

        LocalDateTime aiClassifiedAt = LocalDateTime.of(2025, 1, 1, 10, 0, 12);
        entity.setAiClassifiedAt(aiClassifiedAt);

        entity.setAiProcessed(true);

        // when
        AnimalDetectionDTO dto = mapper.toDTO(entity);

        // then - base fields
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("cat", dto.getSpecies());
        assertEquals("mammal", dto.getAnimalType());
        assertEquals(0.95f, dto.getConfidence(), 0.0001f);
        assertEquals("feline", dto.getOriginalSpecies());
        assertEquals(0.88f, dto.getOriginalConfidence(), 0.0001f);

        // then - alternate species parsed
        assertNotNull(dto.getAlternateSpecies());
        assertEquals(2, dto.getAlternateSpecies().size());
        assertEquals("dog", dto.getAlternateSpecies().get(0).getSpecies());
        assertEquals(0.12, dto.getAlternateSpecies().get(0).getConfidence(), 0.0001);
        assertEquals("fox", dto.getAlternateSpecies().get(1).getSpecies());
        assertEquals(0.05, dto.getAlternateSpecies().get(1).getConfidence(), 0.0001);

        // then - other fields
        assertEquals(10.0f, dto.getX(), 0.0001f);
        assertEquals(20.0f, dto.getY(), 0.0001f);
        assertEquals(100.0f, dto.getWidth(), 0.0001f);
        assertEquals(200.0f, dto.getHeight(), 0.0001f);
        assertEquals(7, dto.getClassId());
        assertEquals("https://example.com/img.jpg", dto.getImageUrl());
        assertEquals(ts, dto.getTimestamp());
        assertEquals("dev-1", dto.getDeviceId());
        assertEquals("Berlin", dto.getLocation());
        assertEquals("motion", dto.getTriggerReason());
        assertEquals(processedAt, dto.getProcessedAt());
        assertEquals(aiClassifiedAt, dto.getAiClassifiedAt());
        assertTrue(dto.isAiProcessed());
    }

    @Test
    void toDTO_shouldSetEmptyAlternateSpecies_whenJsonIsInvalid() {
        // given
        AnimalDetection entity = new AnimalDetection();
        entity.setId(99L);
        entity.setSpecies("cat");
        entity.setAnimalType("mammal");
        entity.setConfidence(0.5f);
        entity.setX(1f);
        entity.setY(1f);
        entity.setWidth(1f);
        entity.setHeight(1f);
        entity.setTimestamp(LocalDateTime.of(2025, 1, 1, 0, 0));

        entity.setAlternateSpecies("{not-valid-json");

        // when
        AnimalDetectionDTO dto = mapper.toDTO(entity);

        // then
        assertNotNull(dto);
        assertNotNull(dto.getAlternateSpecies(), "alternateSpecies should be set (empty list) on parse error");
        assertTrue(dto.getAlternateSpecies().isEmpty());
    }

    @Test
    void toDTO_shouldNotSetAlternateSpecies_whenJsonIsNullOrEmpty() {
        AnimalDetection e1 = new AnimalDetection();
        e1.setId(1L);
        e1.setSpecies("cat");
        e1.setAnimalType("mammal");
        e1.setConfidence(0.5f);
        e1.setX(1f);
        e1.setY(1f);
        e1.setWidth(1f);
        e1.setHeight(1f);
        e1.setTimestamp(LocalDateTime.of(2025, 1, 1, 0, 0));
        e1.setAlternateSpecies(null);

        AnimalDetection e2 = new AnimalDetection();
        e2.setId(2L);
        e2.setSpecies("cat");
        e2.setAnimalType("mammal");
        e2.setConfidence(0.5f);
        e2.setX(1f);
        e2.setY(1f);
        e2.setWidth(1f);
        e2.setHeight(1f);
        e2.setTimestamp(LocalDateTime.of(2025, 1, 1, 0, 0));
        e2.setAlternateSpecies("");

        AnimalDetectionDTO d1 = mapper.toDTO(e1);
        AnimalDetectionDTO d2 = mapper.toDTO(e2);

        assertNotNull(d1);
        assertNotNull(d2);

        // Mapper setzt alternateSpecies nur wenn String != null && !isEmpty
        assertNull(d1.getAlternateSpecies());
        assertNull(d2.getAlternateSpecies());
    }

    @Test
    void toEntity_shouldMapAllFields_andSerializeAlternateSpeciesToJson() {
        // given
        AnimalDetectionDTO dto = new AnimalDetectionDTO();
        dto.setId(2L);
        dto.setSpecies("bird");
        dto.setAnimalType("bird");
        dto.setConfidence(0.77f);
        dto.setOriginalSpecies("sparrow");
        dto.setOriginalConfidence(0.55f);

        AnimalDetectionDTO.AlternateSpeciesDTO alt1 = new AnimalDetectionDTO.AlternateSpeciesDTO();
        alt1.setSpecies("crow");
        alt1.setConfidence(0.11f);

        AnimalDetectionDTO.AlternateSpeciesDTO alt2 = new AnimalDetectionDTO.AlternateSpeciesDTO();
        alt2.setSpecies("pigeon");
        alt2.setConfidence(0.09f);

        dto.setAlternateSpecies(List.of(alt1, alt2));

        dto.setX(1.0f);
        dto.setY(2.0f);
        dto.setWidth(3.0f);
        dto.setHeight(4.0f);
        dto.setClassId(9);
        dto.setImageUrl("url");

        LocalDateTime ts = LocalDateTime.of(2025, 2, 2, 2, 2, 2);
        dto.setTimestamp(ts);

        dto.setDeviceId("dev-2");
        dto.setLocation("Hamburg");
        dto.setTriggerReason("sound");

        LocalDateTime processedAt = LocalDateTime.of(2025, 2, 2, 2, 2, 10);
        dto.setProcessedAt(processedAt);

        LocalDateTime aiClassifiedAt = LocalDateTime.of(2025, 2, 2, 2, 2, 12);
        dto.setAiClassifiedAt(aiClassifiedAt);

        dto.setAiProcessed(false);

        // when
        AnimalDetection entity = mapper.toEntity(dto);

        // then
        assertNotNull(entity);
        assertEquals(2L, entity.getId());
        assertEquals("bird", entity.getSpecies());
        assertEquals("bird", entity.getAnimalType());
        assertEquals(0.77f, entity.getConfidence(), 0.0001f);
        assertEquals("sparrow", entity.getOriginalSpecies());
        assertEquals(0.55f, entity.getOriginalConfidence(), 0.0001f);

        assertNotNull(entity.getAlternateSpecies());
        assertTrue(entity.getAlternateSpecies().contains("\"species\":\"crow\""));
        assertTrue(entity.getAlternateSpecies().contains("\"species\":\"pigeon\""));

        assertEquals(1.0f, entity.getX(), 0.0001f);
        assertEquals(2.0f, entity.getY(), 0.0001f);
        assertEquals(3.0f, entity.getWidth(), 0.0001f);
        assertEquals(4.0f, entity.getHeight(), 0.0001f);
        assertEquals(9, entity.getClassId());
        assertEquals("url", entity.getImageUrl());
        assertEquals(ts, entity.getTimestamp());
        assertEquals("dev-2", entity.getDeviceId());
        assertEquals("Hamburg", entity.getLocation());
        assertEquals("sound", entity.getTriggerReason());
        assertEquals(processedAt, entity.getProcessedAt());
        assertEquals(aiClassifiedAt, entity.getAiClassifiedAt());
        assertFalse(entity.isAiProcessed());
    }

    @Test
    void toDTOList_shouldReturnNull_whenInputIsNull() {
        assertNull(mapper.toDTOList(null));
    }

    @Test
    void toDTOList_shouldMapEachElement() {
        AnimalDetection e1 = new AnimalDetection();
        e1.setId(1L);
        e1.setSpecies("a");
        e1.setAnimalType("mammal");
        e1.setConfidence(0.1f);
        e1.setX(1f);
        e1.setY(1f);
        e1.setWidth(1f);
        e1.setHeight(1f);
        e1.setTimestamp(LocalDateTime.of(2025, 1, 1, 0, 0));

        AnimalDetection e2 = new AnimalDetection();
        e2.setId(2L);
        e2.setSpecies("b");
        e2.setAnimalType("bird");
        e2.setConfidence(0.2f);
        e2.setX(2f);
        e2.setY(2f);
        e2.setWidth(2f);
        e2.setHeight(2f);
        e2.setTimestamp(LocalDateTime.of(2025, 1, 1, 0, 0));

        List<AnimalDetectionDTO> result = mapper.toDTOList(List.of(e1, e2));

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
        assertEquals("a", result.get(0).getSpecies());
        assertEquals("b", result.get(1).getSpecies());
    }
}