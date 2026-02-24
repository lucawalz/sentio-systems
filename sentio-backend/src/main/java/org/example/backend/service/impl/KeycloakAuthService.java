package org.example.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.dto.AuthDTOs;
import org.example.backend.service.AuthService;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAuthService implements AuthService {

    private final Keycloak keycloak;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.admin.server-url}")
    private String serverUrl;

    @Value("${keycloak.public-url:http://localhost:8080}")
    private String publicUrl;

    @Value("${keycloak.admin.client-id}")
    private String clientId;

    @Value("${keycloak.admin.client-secret}")
    private String clientSecret;

    @Override
    public void register(AuthDTOs.RegisterRequest request) {
        log.info("Registering user: {}", request.getUsername());
        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEnabled(true);
        user.setEmailVerified(false);
        // No required actions - we handle verification ourselves

        UsersResource usersResource = keycloak.realm(realm).users();

        // Check if username already exists
        if (!usersResource.search(request.getUsername(), true).isEmpty()) {
            log.warn("Username already exists: {}", request.getUsername());
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "Username already exists");
        }

        // Check if email already exists
        if (!usersResource.searchByEmail(request.getEmail(), true).isEmpty()) {
            log.warn("Email already exists: {}", request.getEmail());
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "Email already exists");
        }

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.getPassword());
        credential.setTemporary(false);

        user.setCredentials(Collections.singletonList(credential));

        Response response = usersResource.create(user);

        if (response.getStatus() == 409) {
            log.warn("User already exists: {}", request.getUsername());
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "User already exists");
        }

        if (response.getStatus() != 201) {
            log.error("Failed to register user. Status: {}", response.getStatus());
            throw new RuntimeException("Failed to register user");
        }

        log.info("User registered successfully, pending email verification");
    }

    @Override
    public AuthDTOs.TokenResponse login(String username, String password) {
        log.info("Authenticating user via direct grant: {}", username);
        String tokenEndpoint = serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("grant_type", "password");
        map.add("username", username);
        map.add("password", password);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    tokenEndpoint,
                    org.springframework.http.HttpMethod.POST,
                    entity,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                    });
            return parseTokenResponse(response);
        } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized e) {
            log.warn("Invalid credentials for user: {}", username);
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid credentials");
        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage());
            throw new RuntimeException("Login failed", e);
        }
    }

    @Override
    public synchronized AuthDTOs.TokenResponse refreshToken(String refreshToken) {
        log.info("Refreshing token");
        String tokenEndpoint = serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("grant_type", "refresh_token");
        map.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    tokenEndpoint,
                    org.springframework.http.HttpMethod.POST,
                    entity,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                    });
            return parseTokenResponse(response);
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            throw new RuntimeException("Token refresh failed", e);
        }
    }

    private AuthDTOs.TokenResponse parseTokenResponse(ResponseEntity<Map<String, Object>> response) {
        Map<String, Object> body = response.getBody();

        if (body != null) {
            return new AuthDTOs.TokenResponse(
                    (String) body.get("access_token"),
                    (String) body.get("refresh_token"),
                    (String) body.get("token_type"),
                    ((Number) body.get("expires_in")).longValue());
        }
        return null;
    }

    @Override
    public void logout(String refreshToken) {
        log.info("Logging out user");
        String logoutEndpoint = serverUrl + "/realms/" + realm + "/protocol/openid-connect/logout";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);

        try {
            restTemplate.postForEntity(logoutEndpoint, entity, Void.class);
            log.info("Logout successful");
        } catch (Exception e) {
            log.error("Logout failed: {}", e.getMessage());
            throw new RuntimeException("Logout failed", e);
        }
    }

    @Override
    public void deleteUser(String userId) {
        log.info("Deleting user: {}", userId);
        UsersResource usersResource = keycloak.realm(realm).users();
        Response response = usersResource.delete(userId);

        if (response.getStatus() != 204) {
            log.error("Failed to delete user. Status: {}", response.getStatus());
            throw new RuntimeException("Failed to delete user");
        }
        log.info("User deleted successfully");
    }

    @Override
    public AuthDTOs.UserInfo getUserFromToken(String accessToken) {
        try {
            // JWT format: header.payload.signature
            String[] parts = accessToken.split("\\.");
            if (parts.length != 3) {
                log.error("Invalid JWT format");
                throw new RuntimeException("Invalid token format");
            }

            // Decode the payload (second part)
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode claims = objectMapper.readTree(payload);

            String id = claims.has("sub")
                    ? claims.get("sub").asText()
                    : null;
            String username = claims.has("preferred_username")
                    ? claims.get("preferred_username").asText()
                    : null;
            String email = claims.has("email")
                    ? claims.get("email").asText()
                    : null;

            java.util.List<String> roles = new java.util.ArrayList<>();
            if (claims.has("realm_access") && claims.get("realm_access").has("roles")) {
                claims.get("realm_access").get("roles").forEach(role -> roles.add(role.asText()));
            }

            log.debug("Extracted user info from token: id={}, username={}, email={}, roles={}", id, username, email,
                    roles);
            return new AuthDTOs.UserInfo(id, username, email, roles);
        } catch (Exception e) {
            log.error("Failed to parse JWT token: {}", e.getMessage());
            throw new RuntimeException("Failed to parse token", e);
        }
    }

    @Override
    public AuthDTOs.UserInfo getCurrentUser() {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }

        if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt) {
            org.springframework.security.oauth2.jwt.Jwt jwt = (org.springframework.security.oauth2.jwt.Jwt) authentication
                    .getPrincipal();
            String id = jwt.getSubject();
            String username = jwt.getClaimAsString("preferred_username");
            String email = jwt.getClaimAsString("email");

            java.util.List<String> roles = new java.util.ArrayList<>();
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null && realmAccess.containsKey("roles")) {
                ((java.util.List<?>) realmAccess.get("roles")).forEach(role -> roles.add(role.toString()));
            }

            return new AuthDTOs.UserInfo(id, username, email, roles);
        }

        throw new RuntimeException(
                "Unsupported authentication authentication type: " + authentication.getClass().getName());
    }

    @Override
    public boolean userExistsByEmail(String email) {
        try {
            UsersResource usersResource = keycloak.realm(realm).users();
            var users = usersResource.searchByEmail(email, true);
            return !users.isEmpty();
        } catch (Exception e) {
            log.error("Error checking if user exists by email: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void updatePassword(String email, String newPassword) {
        log.info("Updating password for user with email: {}", email);

        UsersResource usersResource = keycloak.realm(realm).users();
        var users = usersResource.searchByEmail(email, true);

        if (users.isEmpty()) {
            throw new RuntimeException("User not found with email: " + email);
        }

        String userId = users.get(0).getId();

        // Create new credential
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(newPassword);
        credential.setTemporary(false);

        // Update the user's password
        usersResource.get(userId).resetPassword(credential);

        log.info("Password updated successfully for user: {}", email);
    }

    @Override
    public void markEmailVerified(String email) {
        log.info("Marking email as verified for: {}", email);

        UsersResource usersResource = keycloak.realm(realm).users();
        var users = usersResource.searchByEmail(email, true);

        if (users.isEmpty()) {
            throw new RuntimeException("User not found with email: " + email);
        }

        UserRepresentation user = users.get(0);
        user.setEmailVerified(true);
        // Remove VERIFY_EMAIL required action if present
        if (user.getRequiredActions() != null) {
            user.getRequiredActions().remove("VERIFY_EMAIL");
        }

        usersResource.get(user.getId()).update(user);
        log.info("Email verified successfully for: {}", email);
    }
}
