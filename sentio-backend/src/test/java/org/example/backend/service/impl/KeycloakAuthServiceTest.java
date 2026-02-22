package org.example.backend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import jakarta.ws.rs.core.Response;
import org.example.backend.dto.AuthDTOs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for {@link KeycloakAuthService}.
 * Uses WireMock to mock Keycloak token endpoints and Mockito for Keycloak admin client.
 *
 * <p>
 * Following FIRST principles:
 * </p>
 * <ul>
 * <li><b>Fast</b> - All external calls to Keycloak and REST endpoints are mocked</li>
 * <li><b>Independent</b> - Each test is self-contained with fresh mocks</li>
 * <li><b>Repeatable</b> - No network or state dependencies</li>
 * <li><b>Self-validating</b> - Clear assertions with descriptive test names</li>
 * <li><b>Timely</b> - Tests cover critical authentication flows</li>
 * </ul>
 */
@WireMockTest(httpPort = 8093)
@ExtendWith(MockitoExtension.class)
class KeycloakAuthServiceTest {

    @Mock
    private Keycloak keycloak;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private RealmResource realmResource;

    @Mock
    private UsersResource usersResource;

    @InjectMocks
    private KeycloakAuthService keycloakAuthService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(keycloakAuthService, "realm", "sentio");
        ReflectionTestUtils.setField(keycloakAuthService, "serverUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(keycloakAuthService, "publicUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(keycloakAuthService, "clientId", "sentio-backend");
        ReflectionTestUtils.setField(keycloakAuthService, "clientSecret", "test-secret");
    }

    @Nested
    @DisplayName("register")
    class RegisterTests {

        @Test
        @DisplayName("should register new user successfully")
        void shouldRegisterNewUserSuccessfully() {
            // Given
            AuthDTOs.RegisterRequest request = new AuthDTOs.RegisterRequest();
            request.setUsername("newuser");
            request.setEmail("newuser@test.com");
            request.setPassword("password123");
            request.setFirstName("New");
            request.setLastName("User");

            when(keycloak.realm("sentio")).thenReturn(realmResource);
            when(realmResource.users()).thenReturn(usersResource);
            when(usersResource.search("newuser", true)).thenReturn(Collections.emptyList());
            when(usersResource.searchByEmail("newuser@test.com", true)).thenReturn(Collections.emptyList());

            Response mockResponse = mock(Response.class);
            when(mockResponse.getStatus()).thenReturn(201);
            when(usersResource.create(any(UserRepresentation.class))).thenReturn(mockResponse);

            // When
            keycloakAuthService.register(request);

            // Then
            ArgumentCaptor<UserRepresentation> userCaptor = ArgumentCaptor.forClass(UserRepresentation.class);
            verify(usersResource).create(userCaptor.capture());

            UserRepresentation capturedUser = userCaptor.getValue();
            assertThat(capturedUser.getUsername()).isEqualTo("newuser");
            assertThat(capturedUser.getEmail()).isEqualTo("newuser@test.com");
            assertThat(capturedUser.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("should throw conflict exception when username exists")
        void shouldThrowConflictWhenUsernameExists() {
            // Given
            AuthDTOs.RegisterRequest request = new AuthDTOs.RegisterRequest();
            request.setUsername("existinguser");
            request.setEmail("new@test.com");
            request.setPassword("password");

            UserRepresentation existingUser = new UserRepresentation();
            existingUser.setUsername("existinguser");

            when(keycloak.realm("sentio")).thenReturn(realmResource);
            when(realmResource.users()).thenReturn(usersResource);
            when(usersResource.search("existinguser", true)).thenReturn(List.of(existingUser));

            // When / Then
            assertThatThrownBy(() -> keycloakAuthService.register(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Username already exists");
        }

        @Test
        @DisplayName("should throw conflict exception when email exists")
        void shouldThrowConflictWhenEmailExists() {
            // Given
            AuthDTOs.RegisterRequest request = new AuthDTOs.RegisterRequest();
            request.setUsername("newuser");
            request.setEmail("existing@test.com");
            request.setPassword("password");

            UserRepresentation existingUser = new UserRepresentation();
            existingUser.setEmail("existing@test.com");

            when(keycloak.realm("sentio")).thenReturn(realmResource);
            when(realmResource.users()).thenReturn(usersResource);
            when(usersResource.search("newuser", true)).thenReturn(Collections.emptyList());
            when(usersResource.searchByEmail("existing@test.com", true)).thenReturn(List.of(existingUser));

            // When / Then
            assertThatThrownBy(() -> keycloakAuthService.register(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Email already exists");
        }

        @Test
        @DisplayName("should throw exception when Keycloak returns 409 conflict")
        void shouldThrowExceptionOnKeycloak409() {
            // Given
            AuthDTOs.RegisterRequest request = new AuthDTOs.RegisterRequest();
            request.setUsername("user");
            request.setEmail("user@test.com");
            request.setPassword("password");

            when(keycloak.realm("sentio")).thenReturn(realmResource);
            when(realmResource.users()).thenReturn(usersResource);
            when(usersResource.search(anyString(), eq(true))).thenReturn(Collections.emptyList());
            when(usersResource.searchByEmail(anyString(), eq(true))).thenReturn(Collections.emptyList());

            Response mockResponse = mock(Response.class);
            when(mockResponse.getStatus()).thenReturn(409);
            when(usersResource.create(any(UserRepresentation.class))).thenReturn(mockResponse);

            // When / Then
            assertThatThrownBy(() -> keycloakAuthService.register(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("User already exists");
        }
    }

    @Nested
    @DisplayName("login")
    class LoginTests {

        @Test
        @DisplayName("should return tokens on successful login")
        void shouldReturnTokensOnSuccessfulLogin() {
            // Given
            String username = "testuser";
            String password = "password123";

            Map<String, Object> tokenResponse = Map.of(
                    "access_token", "test-access-token",
                    "refresh_token", "test-refresh-token",
                    "token_type", "Bearer",
                    "expires_in", 3600);

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    any(ParameterizedTypeReference.class))).thenReturn(ResponseEntity.ok(tokenResponse));

            // When
            AuthDTOs.TokenResponse response = keycloakAuthService.login(username, password);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("test-access-token");
            assertThat(response.getRefreshToken()).isEqualTo("test-refresh-token");
            assertThat(response.getExpiresIn()).isEqualTo(3600L);
        }

        @Test
        @DisplayName("should throw unauthorized exception on invalid credentials")
        void shouldThrowUnauthorizedOnInvalidCredentials() {
            // Given
            String username = "testuser";
            String password = "wrongpassword";

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    any(ParameterizedTypeReference.class))).thenThrow(HttpClientErrorException.Unauthorized.create(
                            HttpStatus.UNAUTHORIZED, "Unauthorized", null, null, null));

            // When / Then
            assertThatThrownBy(() -> keycloakAuthService.login(username, password))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Invalid credentials");
        }

        @Test
        @DisplayName("should call correct token endpoint")
        void shouldCallCorrectTokenEndpoint() {
            // Given
            Map<String, Object> tokenResponse = Map.of(
                    "access_token", "token",
                    "refresh_token", "refresh",
                    "token_type", "Bearer",
                    "expires_in", 3600);

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    any(ParameterizedTypeReference.class))).thenReturn(ResponseEntity.ok(tokenResponse));

            // When
            keycloakAuthService.login("user", "pass");

            // Then
            verify(restTemplate).exchange(
                    eq("http://localhost:8080/realms/sentio/protocol/openid-connect/token"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    any(ParameterizedTypeReference.class));
        }
    }

    @Nested
    @DisplayName("refreshToken")
    class RefreshTokenTests {

        @Test
        @DisplayName("should return new tokens on successful refresh")
        void shouldReturnNewTokensOnSuccessfulRefresh() {
            // Given
            String refreshToken = "valid-refresh-token";

            Map<String, Object> tokenResponse = Map.of(
                    "access_token", "new-access-token",
                    "refresh_token", "new-refresh-token",
                    "token_type", "Bearer",
                    "expires_in", 3600);

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    any(ParameterizedTypeReference.class))).thenReturn(ResponseEntity.ok(tokenResponse));

            // When
            AuthDTOs.TokenResponse response = keycloakAuthService.refreshToken(refreshToken);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("new-access-token");
            assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        }

        @Test
        @DisplayName("should throw exception on invalid refresh token")
        void shouldThrowExceptionOnInvalidRefreshToken() {
            // Given
            String invalidRefreshToken = "invalid-refresh-token";

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    any(ParameterizedTypeReference.class))).thenThrow(new RuntimeException("invalid_grant"));

            // When / Then
            assertThatThrownBy(() -> keycloakAuthService.refreshToken(invalidRefreshToken))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Token refresh failed");
        }
    }

    @Nested
    @DisplayName("logout")
    class LogoutTests {

        @Test
        @DisplayName("should call logout endpoint successfully")
        void shouldCallLogoutEndpointSuccessfully() {
            // Given
            String refreshToken = "valid-refresh-token";

            when(restTemplate.postForEntity(
                    anyString(),
                    any(HttpEntity.class),
                    eq(Void.class))).thenReturn(ResponseEntity.noContent().build());

            // When
            keycloakAuthService.logout(refreshToken);

            // Then
            verify(restTemplate).postForEntity(
                    eq("http://localhost:8080/realms/sentio/protocol/openid-connect/logout"),
                    any(HttpEntity.class),
                    eq(Void.class));
        }

        @Test
        @DisplayName("should throw exception on logout failure")
        void shouldThrowExceptionOnLogoutFailure() {
            // Given
            String refreshToken = "valid-refresh-token";

            when(restTemplate.postForEntity(
                    anyString(),
                    any(HttpEntity.class),
                    eq(Void.class))).thenThrow(new RuntimeException("Network error"));

            // When / Then
            assertThatThrownBy(() -> keycloakAuthService.logout(refreshToken))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Logout failed");
        }
    }

    @Nested
    @DisplayName("deleteUser")
    class DeleteUserTests {

        @Test
        @DisplayName("should delete user successfully")
        void shouldDeleteUserSuccessfully() {
            // Given
            String userId = "user-123";

            when(keycloak.realm("sentio")).thenReturn(realmResource);
            when(realmResource.users()).thenReturn(usersResource);

            Response mockResponse = mock(Response.class);
            when(mockResponse.getStatus()).thenReturn(204);
            when(usersResource.delete(userId)).thenReturn(mockResponse);

            // When
            keycloakAuthService.deleteUser(userId);

            // Then
            verify(usersResource).delete(userId);
        }

        @Test
        @DisplayName("should throw exception on deletion failure")
        void shouldThrowExceptionOnDeletionFailure() {
            // Given
            String userId = "user-123";

            when(keycloak.realm("sentio")).thenReturn(realmResource);
            when(realmResource.users()).thenReturn(usersResource);

            Response mockResponse = mock(Response.class);
            when(mockResponse.getStatus()).thenReturn(404);
            when(usersResource.delete(userId)).thenReturn(mockResponse);

            // When / Then
            assertThatThrownBy(() -> keycloakAuthService.deleteUser(userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to delete user");
        }
    }

    @Nested
    @DisplayName("getUserFromToken")
    class GetUserFromTokenTests {

        @Test
        @DisplayName("should parse JWT and return user info")
        void shouldParseJwtAndReturnUserInfo() throws Exception {
            // Given
            // JWT format: header.payload.signature
            // This is a valid base64url encoded payload
            String header = java.util.Base64.getUrlEncoder().withoutPadding()
                    .encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes());
            String payload = java.util.Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(
                            "{\"preferred_username\":\"testuser\",\"email\":\"test@example.com\",\"realm_access\":{\"roles\":[\"user\",\"admin\"]}}"
                                    .getBytes());
            String signature = "fake-signature";
            String accessToken = header + "." + payload + "." + signature;

            // Create a real ObjectMapper for this test since we're testing JSON parsing
            ReflectionTestUtils.setField(keycloakAuthService, "objectMapper", new ObjectMapper());

            // When
            AuthDTOs.UserInfo userInfo = keycloakAuthService.getUserFromToken(accessToken);

            // Then
            assertThat(userInfo).isNotNull();
            assertThat(userInfo.getUsername()).isEqualTo("testuser");
            assertThat(userInfo.getEmail()).isEqualTo("test@example.com");
            assertThat(userInfo.getRoles()).containsExactlyInAnyOrder("user", "admin");
        }

        @Test
        @DisplayName("should throw exception on invalid JWT format")
        void shouldThrowExceptionOnInvalidJwtFormat() {
            // Given
            String invalidToken = "not-a-valid-jwt";

            // When / Then
            assertThatThrownBy(() -> keycloakAuthService.getUserFromToken(invalidToken))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to parse token");
        }

        @Test
        @DisplayName("should handle JWT without optional claims")
        void shouldHandleJwtWithoutOptionalClaims() throws Exception {
            // Given - JWT with minimal claims
            String header = java.util.Base64.getUrlEncoder().withoutPadding()
                    .encodeToString("{\"alg\":\"RS256\"}".getBytes());
            String payload = java.util.Base64.getUrlEncoder().withoutPadding()
                    .encodeToString("{}".getBytes());
            String signature = "sig";
            String accessToken = header + "." + payload + "." + signature;

            ReflectionTestUtils.setField(keycloakAuthService, "objectMapper", new ObjectMapper());

            // When
            AuthDTOs.UserInfo userInfo = keycloakAuthService.getUserFromToken(accessToken);

            // Then
            assertThat(userInfo).isNotNull();
            assertThat(userInfo.getUsername()).isNull();
            assertThat(userInfo.getEmail()).isNull();
            assertThat(userInfo.getRoles()).isEmpty();
        }
    }
}