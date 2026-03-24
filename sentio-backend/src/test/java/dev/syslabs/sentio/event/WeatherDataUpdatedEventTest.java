package dev.syslabs.sentio.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WeatherDataUpdatedEvent Unit Tests")
class WeatherDataUpdatedEventTest {

    @Test
    @DisplayName("Should correctly instantiate and return properties")
    void shouldReturnCorrectProperties() {
        Object source = new Object();
        WeatherDataUpdatedEvent.DataType dataType = WeatherDataUpdatedEvent.DataType.FORECAST;
        String deviceId = "device-123";
        Double lat = 52.52;
        Double lon = 13.40;

        WeatherDataUpdatedEvent event = new WeatherDataUpdatedEvent(source, dataType, deviceId, lat, lon);

        assertThat(event.getSource()).isEqualTo(source);
        assertThat(event.getDataType()).isEqualTo(dataType);
        assertThat(event.getDeviceId()).isEqualTo(deviceId);
        assertThat(event.getLatitude()).isEqualTo(lat);
        assertThat(event.getLongitude()).isEqualTo(lon);
    }
}
