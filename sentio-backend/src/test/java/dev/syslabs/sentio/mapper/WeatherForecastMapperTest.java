package dev.syslabs.sentio.mapper;

import dev.syslabs.sentio.dto.WeatherForecastDTO;
import dev.syslabs.sentio.model.WeatherForecast;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WeatherForecastMapperTest {

    private WeatherForecastMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new WeatherForecastMapper();
    }

    @Test
    void toDTO_shouldReturnNull_whenEntityIsNull() {
        assertNull(mapper.toDTO(null));
    }

    @Test
    void toDTO_shouldMapAllFields() {
        // given
        WeatherForecast entity = new WeatherForecast();
        entity.setId(1L);
        entity.setForecastDate(LocalDate.of(2025, 1, 1));
        entity.setForecastDateTime(LocalDateTime.of(2025, 1, 1, 12, 0));

        LocalDateTime createdAt = LocalDateTime.of(2025, 1, 1, 9, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2025, 1, 1, 10, 0);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(updatedAt);

        // Core
        entity.setTemperature(12.3f);
        entity.setHumidity(65.0f);
        entity.setApparentTemperature(11.0f);
        entity.setPressure(1013.5f);
        entity.setDescription("Partly cloudy");
        entity.setWeatherMain("Clouds");
        entity.setIcon("03d");

        // Wind
        entity.setWindSpeed(5.5f);
        entity.setWindDirection(210.0f);
        entity.setWindGusts(9.2f);
        entity.setCloudCover(55.0f);
        entity.setVisibility(10_000f);

        // Precipitation
        entity.setPrecipitation(0.8f);
        entity.setRain(0.5f);
        entity.setShowers(0.2f);
        entity.setSnowfall(0.0f);
        entity.setSnowDepth(0.0f);
        entity.setDewPoint(7.7f);
        entity.setPrecipitationProbability(30.0f);
        entity.setWeatherCode(3);

        // Location
        entity.setCity("Berlin");
        entity.setCountry("Germany");
        entity.setLatitude(52.52f);
        entity.setLongitude(13.405f);
        entity.setDetectedLocation("Berlin, Germany");
        entity.setIpAddress("1.2.3.4"); // wird im Mapper NICHT gemappt

        // when
        WeatherForecastDTO dto = mapper.toDTO(entity);

        // then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(LocalDate.of(2025, 1, 1), dto.getForecastDate());
        assertEquals(LocalDateTime.of(2025, 1, 1, 12, 0), dto.getForecastDateTime());
        assertEquals(createdAt, dto.getCreatedAt());
        assertEquals(updatedAt, dto.getUpdatedAt());

        assertEquals(12.3f, dto.getTemperature());
        assertEquals(65.0f, dto.getHumidity());
        assertEquals(11.0f, dto.getApparentTemperature());
        assertEquals(1013.5f, dto.getPressure());
        assertEquals("Partly cloudy", dto.getDescription());
        assertEquals("Clouds", dto.getWeatherMain());
        assertEquals("03d", dto.getIcon());

        assertEquals(5.5f, dto.getWindSpeed());
        assertEquals(210.0f, dto.getWindDirection());
        assertEquals(9.2f, dto.getWindGusts());
        assertEquals(55.0f, dto.getCloudCover());
        assertEquals(10_000f, dto.getVisibility());

        assertEquals(0.8f, dto.getPrecipitation());
        assertEquals(0.5f, dto.getRain());
        assertEquals(0.2f, dto.getShowers());
        assertEquals(0.0f, dto.getSnowfall());
        assertEquals(0.0f, dto.getSnowDepth());
        assertEquals(7.7f, dto.getDewPoint());
        assertEquals(30.0f, dto.getPrecipitationProbability());
        assertEquals(3, dto.getWeatherCode());

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
        WeatherForecast e1 = new WeatherForecast();
        e1.setId(1L);
        e1.setForecastDate(LocalDate.of(2025, 1, 1));
        e1.setForecastDateTime(LocalDateTime.of(2025, 1, 1, 10, 0));
        e1.setCreatedAt(LocalDateTime.of(2025, 1, 1, 9, 0));
        e1.setUpdatedAt(LocalDateTime.of(2025, 1, 1, 9, 30));
        e1.setCity("Berlin");
        e1.setCountry("Germany");

        WeatherForecast e2 = new WeatherForecast();
        e2.setId(2L);
        e2.setForecastDate(LocalDate.of(2025, 1, 2));
        e2.setForecastDateTime(LocalDateTime.of(2025, 1, 2, 10, 0));
        e2.setCreatedAt(LocalDateTime.of(2025, 1, 2, 9, 0));
        e2.setUpdatedAt(LocalDateTime.of(2025, 1, 2, 9, 30));
        e2.setCity("Hamburg");
        e2.setCountry("Germany");

        // when
        List<WeatherForecastDTO> result = mapper.toDTOList(List.of(e1, e2));

        // then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
        assertEquals(LocalDate.of(2025, 1, 1), result.get(0).getForecastDate());
        assertEquals(LocalDate.of(2025, 1, 2), result.get(1).getForecastDate());
        assertEquals("Berlin", result.get(0).getCity());
        assertEquals("Hamburg", result.get(1).getCity());
    }
}