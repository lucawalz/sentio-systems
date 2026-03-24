package dev.syslabs.sentio.mapper;

import dev.syslabs.sentio.dto.WeatherAlertDTO;
import dev.syslabs.sentio.model.WeatherAlert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WeatherAlertMapperTest {

    private WeatherAlertMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new WeatherAlertMapper();
    }

    @Test
    void toDTO_shouldReturnNull_whenEntityIsNull() {
        assertNull(mapper.toDTO(null, true));
        assertNull(mapper.toDTO(null, false));
    }

    @Test
    void toDTO_shouldMapAllFields_andLocalizeGerman_whenPreferGermanAndGermanExists() {
        // given
        WeatherAlert entity = new WeatherAlert();
        entity.setId(1L);
        entity.setBrightSkyId(101);
        entity.setAlertId("ALERT-1");
        entity.setStatus("actual");

        LocalDateTime effective = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime onset = LocalDateTime.of(2025, 1, 1, 11, 0);
        LocalDateTime expires = LocalDateTime.of(2025, 1, 1, 18, 0);
        entity.setEffective(effective);
        entity.setOnset(onset);
        entity.setExpires(expires);

        entity.setCategory("met");
        entity.setResponseType("monitor");
        entity.setUrgency("immediate");
        entity.setSeverity("severe");
        entity.setCertainty("likely");

        entity.setEventCode(22);
        entity.setEventEn("Storm");
        entity.setEventDe("Sturm");

        entity.setHeadlineEn("Storm warning");
        entity.setHeadlineDe("Sturmwarnung");

        entity.setDescriptionEn("High winds expected.");
        entity.setDescriptionDe("Starker Wind erwartet.");

        entity.setInstructionEn("Stay inside.");
        entity.setInstructionDe("Bleiben Sie drinnen.");

        entity.setWarnCellId(12345L);
        entity.setName("Berlin");
        entity.setNameShort("B");
        entity.setDistrict("Berlin");
        entity.setState("Berlin");
        entity.setStateShort("BE");
        entity.setCity("Berlin");
        entity.setCountry("Germany");
        entity.setLatitude(52.52f);
        entity.setLongitude(13.405f);

        LocalDateTime createdAt = LocalDateTime.of(2025, 1, 1, 9, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2025, 1, 1, 9, 30);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(updatedAt);

        // when
        WeatherAlertDTO dto = mapper.toDTO(entity, true);

        // then - standard field mapping
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(101, dto.getBrightSkyId());
        assertEquals("ALERT-1", dto.getAlertId());
        assertEquals("actual", dto.getStatus());
        assertEquals(effective, dto.getEffective());
        assertEquals(onset, dto.getOnset());
        assertEquals(expires, dto.getExpires());
        assertEquals("met", dto.getCategory());
        assertEquals("monitor", dto.getResponseType());
        assertEquals("immediate", dto.getUrgency());
        assertEquals("severe", dto.getSeverity());
        assertEquals("likely", dto.getCertainty());
        assertEquals(22, dto.getEventCode());
        assertEquals("Storm", dto.getEventEn());
        assertEquals("Sturm", dto.getEventDe());
        assertEquals("Storm warning", dto.getHeadlineEn());
        assertEquals("Sturmwarnung", dto.getHeadlineDe());
        assertEquals("High winds expected.", dto.getDescriptionEn());
        assertEquals("Starker Wind erwartet.", dto.getDescriptionDe());
        assertEquals("Stay inside.", dto.getInstructionEn());
        assertEquals("Bleiben Sie drinnen.", dto.getInstructionDe());
        assertEquals(12345L, dto.getWarnCellId());
        assertEquals("Berlin", dto.getName());
        assertEquals("B", dto.getNameShort());
        assertEquals("Berlin", dto.getDistrict());
        assertEquals("Berlin", dto.getState());
        assertEquals("BE", dto.getStateShort());
        assertEquals("Berlin", dto.getCity());
        assertEquals("Germany", dto.getCountry());
        assertEquals(52.52f, dto.getLatitude());
        assertEquals(13.405f, dto.getLongitude());
        assertEquals(createdAt, dto.getCreatedAt());
        assertEquals(updatedAt, dto.getUpdatedAt());

        // then - localized fields
        assertEquals("Sturmwarnung", dto.getLocalizedHeadline());
        assertEquals("Starker Wind erwartet.", dto.getLocalizedDescription());
        assertEquals("Sturm", dto.getLocalizedEvent());
        assertEquals("Bleiben Sie drinnen.", dto.getLocalizedInstruction());

        // isActive is dynamic (depends on now). We only assert it's set (i.e., no crash).
        // If you want deterministic tests, you'd need a Clock in entity, which you don't have.
        assertNotNull(dto.getIsActive());
    }

    @Test
    void toDTO_shouldFallbackToEnglish_whenPreferGermanButGermanMissing() {
        // given
        WeatherAlert entity = new WeatherAlert();
        entity.setId(2L);
        entity.setAlertId("ALERT-2");
        entity.setEffective(LocalDateTime.of(2025, 1, 1, 10, 0));
        entity.setCreatedAt(LocalDateTime.of(2025, 1, 1, 9, 0));
        entity.setUpdatedAt(LocalDateTime.of(2025, 1, 1, 9, 30));

        entity.setHeadlineEn("English headline");
        entity.setHeadlineDe(null);

        entity.setDescriptionEn("English description");
        entity.setDescriptionDe(null);

        entity.setEventEn("Rain");
        entity.setEventDe(null);

        entity.setInstructionEn("English instruction");
        entity.setInstructionDe(null);

        // when
        WeatherAlertDTO dto = mapper.toDTO(entity, true);

        // then - fallback behavior
        assertNotNull(dto);
        assertEquals("English headline", dto.getLocalizedHeadline());
        assertEquals("English description", dto.getLocalizedDescription());
        assertEquals("Rain", dto.getLocalizedEvent());
        assertEquals("English instruction", dto.getLocalizedInstruction());
    }

    @Test
    void toDTO_shouldUseEnglish_whenPreferGermanFalse() {
        // given
        WeatherAlert entity = new WeatherAlert();
        entity.setId(3L);
        entity.setAlertId("ALERT-3");
        entity.setEffective(LocalDateTime.of(2025, 1, 1, 10, 0));
        entity.setCreatedAt(LocalDateTime.of(2025, 1, 1, 9, 0));
        entity.setUpdatedAt(LocalDateTime.of(2025, 1, 1, 9, 30));

        entity.setHeadlineEn("EN headline");
        entity.setHeadlineDe("DE headline");
        entity.setDescriptionEn("EN desc");
        entity.setDescriptionDe("DE desc");
        entity.setEventEn("Snow");
        entity.setEventDe("Schnee");
        entity.setInstructionEn("EN instr");
        entity.setInstructionDe("DE instr");

        // when
        WeatherAlertDTO dto = mapper.toDTO(entity, false);

        // then
        assertNotNull(dto);
        assertEquals("EN headline", dto.getLocalizedHeadline());
        assertEquals("EN desc", dto.getLocalizedDescription());
        assertEquals("Snow", dto.getLocalizedEvent());
        assertEquals("EN instr", dto.getLocalizedInstruction());
    }

    @Test
    void toDTOList_shouldReturnNull_whenEntitiesListIsNull() {
        assertNull(mapper.toDTOList(null, true));
    }

    @Test
    void toDTOList_shouldMapEachElement_withLocalization() {
        WeatherAlert e1 = new WeatherAlert();
        e1.setId(1L);
        e1.setAlertId("A1");
        e1.setEffective(LocalDateTime.of(2025, 1, 1, 10, 0));
        e1.setCreatedAt(LocalDateTime.of(2025, 1, 1, 9, 0));
        e1.setUpdatedAt(LocalDateTime.of(2025, 1, 1, 9, 30));
        e1.setHeadlineEn("EN1");
        e1.setHeadlineDe("DE1");

        WeatherAlert e2 = new WeatherAlert();
        e2.setId(2L);
        e2.setAlertId("A2");
        e2.setEffective(LocalDateTime.of(2025, 1, 1, 10, 0));
        e2.setCreatedAt(LocalDateTime.of(2025, 1, 1, 9, 0));
        e2.setUpdatedAt(LocalDateTime.of(2025, 1, 1, 9, 30));
        e2.setHeadlineEn("EN2");
        e2.setHeadlineDe("DE2");

        List<WeatherAlertDTO> dtos = mapper.toDTOList(List.of(e1, e2), true);

        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals("A1", dtos.get(0).getAlertId());
        assertEquals("DE1", dtos.get(0).getLocalizedHeadline());
        assertEquals("A2", dtos.get(1).getAlertId());
        assertEquals("DE2", dtos.get(1).getLocalizedHeadline());
    }
}