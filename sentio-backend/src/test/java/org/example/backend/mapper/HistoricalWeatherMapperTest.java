package org.example.backend.mapper;

import org.example.backend.dto.HistoricalWeatherDTO;
import org.example.backend.model.HistoricalWeather;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HistoricalWeatherMapperTest {

    private HistoricalWeatherMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new HistoricalWeatherMapper();
    }

    @Test
    void toDTO_shouldReturnNull_whenEntityIsNull() {
        assertNull(mapper.toDTO(null));
    }

    @Test
    void toDTO_shouldMapAllFields() {
        // given
        HistoricalWeather entity = new HistoricalWeather();
        entity.setId(1L);
        entity.setWeatherDate(LocalDate.of(2025, 1, 5));
        entity.setCreatedAt(LocalDateTime.of(2025, 1, 6, 10, 0, 0));
        entity.setUpdatedAt(LocalDateTime.of(2025, 1, 6, 11, 0, 0));

        entity.setWeatherCode(3);
        entity.setMaxTemperature(12.5f);
        entity.setMinTemperature(4.25f);

        entity.setSunrise(LocalDateTime.of(2025, 1, 5, 8, 12, 0));
        entity.setSunset(LocalDateTime.of(2025, 1, 5, 16, 40, 0));

        entity.setDaylightDuration(8_000f);
        entity.setSunshineDuration(5_000f);
        entity.setUvIndexMax(1.7f);

        entity.setPrecipitationSum(2.3f);
        entity.setPrecipitationHours(1.0f);

        entity.setWindSpeedMax(7.6f);
        entity.setWindDirectionDominant(210.0f);

        entity.setDescription("Partly cloudy");
        entity.setWeatherMain("Clouds");
        entity.setIcon("03d");

        entity.setCity("Berlin");
        entity.setCountry("Germany");
        entity.setLatitude(52.52f);
        entity.setLongitude(13.405f);

        entity.setDetectedLocation("Berlin, Germany");
        entity.setIpAddress("1.2.3.4");

        // when
        HistoricalWeatherDTO dto = mapper.toDTO(entity);

        // then
        assertNotNull(dto);

        assertEquals(1L, dto.getId());
        assertEquals(LocalDate.of(2025, 1, 5), dto.getWeatherDate());
        assertEquals(LocalDateTime.of(2025, 1, 6, 10, 0, 0), dto.getCreatedAt());
        assertEquals(LocalDateTime.of(2025, 1, 6, 11, 0, 0), dto.getUpdatedAt());

        assertEquals(3, dto.getWeatherCode());
        assertEquals(12.5f, dto.getMaxTemperature());
        assertEquals(4.25f, dto.getMinTemperature());

        assertEquals(LocalDateTime.of(2025, 1, 5, 8, 12, 0), dto.getSunrise());
        assertEquals(LocalDateTime.of(2025, 1, 5, 16, 40, 0), dto.getSunset());

        assertEquals(8_000f, dto.getDaylightDuration());
        assertEquals(5_000f, dto.getSunshineDuration());
        assertEquals(1.7f, dto.getUvIndexMax());

        assertEquals(2.3f, dto.getPrecipitationSum());
        assertEquals(1.0f, dto.getPrecipitationHours());

        assertEquals(7.6f, dto.getWindSpeedMax());
        assertEquals(210.0f, dto.getWindDirectionDominant());

        assertEquals("Partly cloudy", dto.getDescription());
        assertEquals("Clouds", dto.getWeatherMain());
        assertEquals("03d", dto.getIcon());

        assertEquals("Berlin", dto.getCity());
        assertEquals("Germany", dto.getCountry());
        assertEquals(52.52f, dto.getLatitude());
        assertEquals(13.405f, dto.getLongitude());

        assertEquals("Berlin, Germany", dto.getDetectedLocation());
    }

    @Test
    void toDTOList_shouldReturnNull_whenEntitiesListIsNull() {
        assertNull(mapper.toDTOList(null));
    }

    @Test
    void toDTOList_shouldMapEachElement() {
        // given
        HistoricalWeather e1 = new HistoricalWeather();
        e1.setId(1L);
        e1.setWeatherDate(LocalDate.of(2025, 1, 1));
        e1.setCreatedAt(LocalDateTime.of(2025, 1, 2, 10, 0));
        e1.setUpdatedAt(LocalDateTime.of(2025, 1, 2, 11, 0));

        HistoricalWeather e2 = new HistoricalWeather();
        e2.setId(2L);
        e2.setWeatherDate(LocalDate.of(2025, 1, 2));
        e2.setCreatedAt(LocalDateTime.of(2025, 1, 3, 10, 0));
        e2.setUpdatedAt(LocalDateTime.of(2025, 1, 3, 11, 0));

        // when
        List<HistoricalWeatherDTO> result = mapper.toDTOList(List.of(e1, e2));

        // then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
        assertEquals(LocalDate.of(2025, 1, 1), result.get(0).getWeatherDate());
        assertEquals(LocalDate.of(2025, 1, 2), result.get(1).getWeatherDate());
    }
}