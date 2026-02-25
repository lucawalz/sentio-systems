package org.example.backend.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("KeycloakConfig Unit Tests")
class KeycloakConfigTest {

    @Test
    @DisplayName("Should create Keycloak client bean successfully")
    void shouldCreateKeycloakClient() {
        KeycloakConfig config = new KeycloakConfig();
        ReflectionTestUtils.setField(config, "serverUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(config, "realm", "test-realm");
        ReflectionTestUtils.setField(config, "clientId", "test-client");
        ReflectionTestUtils.setField(config, "clientSecret", "test-secret");

        Keycloak keycloak = config.keycloak();
        assertThat(keycloak).isNotNull();
    }

    @Test
    @DisplayName("Should throw exception when creating Keycloak client bean with null secret")
    void shouldCreateKeycloakClient_NullSecret() {
        KeycloakConfig config = new KeycloakConfig();
        ReflectionTestUtils.setField(config, "serverUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(config, "realm", "test-realm");
        ReflectionTestUtils.setField(config, "clientId", "test-client");
        ReflectionTestUtils.setField(config, "clientSecret", null);

        assertThatThrownBy(() -> config.keycloak())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("clientSecret required");
    }

}
