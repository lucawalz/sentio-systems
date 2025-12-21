package org.example.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.backend.model.LocationData;
import org.example.backend.repository.LocationDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link IpLocationService}.
 * 
 * <p>
 * Following FIRST principles with Given/When/Then format.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class IpLocationServiceTest {

    @Mock
    private LocationDataRepository locationDataRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private IpLocationService ipLocationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(ipLocationService, "ipLocationApiUrl", "http://ip-api.com/json/");
    }

    @Nested
    @DisplayName("getLocationByIp")
    class GetLocationByIpTests {

        @Test
        @DisplayName("should return cached location if less than 24 hours old")
        void shouldReturnCachedLocationIfFresh() {
            // Given
            String ipAddress = "192.168.1.1";
            LocationData cachedLocation = new LocationData();
            cachedLocation.setIpAddress(ipAddress);
            cachedLocation.setCity("Berlin");
            cachedLocation.setCountry("Germany");
            cachedLocation.setUpdatedAt(LocalDateTime.now().minusHours(1));

            when(locationDataRepository.findLatestByIpAddress(ipAddress))
                    .thenReturn(Optional.of(cachedLocation));

            // When
            Optional<LocationData> result = ipLocationService.getLocationByIp(ipAddress);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getCity()).isEqualTo("Berlin");
            verify(restTemplate, never()).getForObject(anyString(), any());
        }

        @Test
        @DisplayName("should fetch fresh data when cache is older than 24 hours")
        void shouldFetchFreshDataWhenCacheStale() throws Exception {
            // Given
            String ipAddress = "192.168.1.1";
            LocationData staleLocation = new LocationData();
            staleLocation.setUpdatedAt(LocalDateTime.now().minusHours(25));

            when(locationDataRepository.findLatestByIpAddress(ipAddress))
                    .thenReturn(Optional.of(staleLocation));

            String apiResponse = """
                    {
                        "status": "success",
                        "city": "Munich",
                        "country": "Germany",
                        "regionName": "Bavaria",
                        "lat": 48.1351,
                        "lon": 11.5820,
                        "timezone": "Europe/Berlin",
                        "isp": "TestISP",
                        "org": "TestOrg"
                    }
                    """;

            when(restTemplate.getForObject(eq("http://ip-api.com/json/" + ipAddress), eq(String.class)))
                    .thenReturn(apiResponse);

            ObjectMapper realMapper = new ObjectMapper();
            JsonNode jsonNode = realMapper.readTree(apiResponse);
            when(objectMapper.readTree(apiResponse)).thenReturn(jsonNode);

            when(locationDataRepository.save(any(LocationData.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            Optional<LocationData> result = ipLocationService.getLocationByIp(ipAddress);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getCity()).isEqualTo("Munich");
            verify(locationDataRepository).save(any(LocationData.class));
        }

        @Test
        @DisplayName("should return empty when API returns failure status")
        void shouldReturnEmptyOnApiFailure() throws Exception {
            // Given
            String ipAddress = "invalid";
            when(locationDataRepository.findLatestByIpAddress(ipAddress))
                    .thenReturn(Optional.empty());

            String apiResponse = """
                    {
                        "status": "fail",
                        "message": "invalid query"
                    }
                    """;

            when(restTemplate.getForObject(anyString(), eq(String.class)))
                    .thenReturn(apiResponse);

            ObjectMapper realMapper = new ObjectMapper();
            when(objectMapper.readTree(apiResponse)).thenReturn(realMapper.readTree(apiResponse));

            // When
            Optional<LocationData> result = ipLocationService.getLocationByIp(ipAddress);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty on exception")
        void shouldReturnEmptyOnException() {
            // Given
            String ipAddress = "192.168.1.1";
            when(locationDataRepository.findLatestByIpAddress(ipAddress))
                    .thenReturn(Optional.empty());
            when(restTemplate.getForObject(anyString(), eq(String.class)))
                    .thenThrow(new RuntimeException("Network error"));

            // When
            Optional<LocationData> result = ipLocationService.getLocationByIp(ipAddress);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getCurrentLocation")
    class GetCurrentLocationTests {

        @Test
        @DisplayName("should return empty on network error")
        void shouldReturnEmptyOnNetworkError() {
            // Given
            when(restTemplate.getForObject(eq("http://ip-api.com/json/"), eq(String.class)))
                    .thenThrow(new ResourceAccessException("Connection refused"));

            // When
            Optional<LocationData> result = ipLocationService.getCurrentLocation();

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("cleanupOldLocationData")
    class CleanupOldLocationDataTests {

        @Test
        @DisplayName("should delete location data older than 14 days")
        void shouldDeleteOldLocationData() {
            // Given - service is set up

            // When
            ipLocationService.cleanupOldLocationData();

            // Then
            ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(locationDataRepository).deleteOldLocationData(cutoffCaptor.capture());

            LocalDateTime capturedCutoff = cutoffCaptor.getValue();
            assertThat(capturedCutoff).isBefore(LocalDateTime.now().minusDays(13));
            assertThat(capturedCutoff).isAfter(LocalDateTime.now().minusDays(15));
        }
    }
}
