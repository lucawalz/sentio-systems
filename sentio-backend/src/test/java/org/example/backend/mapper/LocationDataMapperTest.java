package org.example.backend.mapper;

import org.example.backend.dto.LocationDataDTO;
import org.example.backend.model.LocationData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class LocationDataMapperTest {

    private LocationDataMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new LocationDataMapper();
    }

    @Test
    void toDTO_shouldReturnNull_whenEntityIsNull() {
        assertNull(mapper.toDTO(null));
    }

    @Test
    void toDTO_shouldMapAllFields() {
        // given
        LocationData entity = new LocationData();
        entity.setId(1L);
        entity.setIpAddress("1.2.3.4");
        entity.setCity("Berlin");
        entity.setCountry("Germany");
        entity.setRegion("Berlin");
        entity.setLatitude(52.52f);
        entity.setLongitude(13.405f);
        entity.setTimezone("Europe/Berlin");
        entity.setIsp("ExampleISP");
        entity.setOrganization("ExampleOrg");

        LocalDateTime createdAt = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2025, 1, 1, 12, 0, 0);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(updatedAt);

        // when
        LocationDataDTO dto = mapper.toDTO(entity);

        // then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("1.2.3.4", dto.getIpAddress());
        assertEquals("Berlin", dto.getCity());
        assertEquals("Germany", dto.getCountry());
        assertEquals("Berlin", dto.getRegion());
        assertEquals(52.52f, dto.getLatitude());
        assertEquals(13.405f, dto.getLongitude());
        assertEquals("Europe/Berlin", dto.getTimezone());
        assertEquals("ExampleISP", dto.getIsp());
        assertEquals("ExampleOrg", dto.getOrganization());
        assertEquals(createdAt, dto.getCreatedAt());
        assertEquals(updatedAt, dto.getUpdatedAt());
    }
}