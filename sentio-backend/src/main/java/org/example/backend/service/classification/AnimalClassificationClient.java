package org.example.backend.service.classification;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.Map;

/**
 * Client component responsible for communication with the preprocessing/classification API.
 * Handles multipart upload requests and response validation for AI classification.
 * <p>
 * This component isolates HTTP transport concerns from service orchestration logic,
 * improving testability and supporting future client substitutions.
 * </p>
 *
 * @author Sentio Team
 * @version 1.0
 * @since 1.0
 */
@Component
@Slf4j
public class AnimalClassificationClient {

    private final RestTemplate restTemplate;

    @Value("${preprocessing.service.url}")
    private String preprocessingServiceUrl;

    public AnimalClassificationClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Calls the preprocessing service and requests AI classification for a single image.
     *
     * @param imageFile  Local image file to upload
     * @param animalType Target animal type for classifier routing
     * @return Response body map when request succeeds, null otherwise
     */
    @CircuitBreaker(name = "aiClassifier", fallbackMethod = "callPreprocessingServiceFallback")
    public Map<String, Object> callPreprocessingService(File imageFile, String animalType) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(imageFile));
            body.add("animal_type", animalType);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            log.debug("Calling preprocessing service at: {} for animal type: {}", preprocessingServiceUrl, animalType);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    preprocessingServiceUrl,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    });

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();

                if (responseBody.containsKey("preprocessing_applied")) {
                    log.info("Image preprocessing applied - Original: {} bytes, Enhanced: {} bytes",
                            responseBody.get("original_size_bytes"),
                            responseBody.get("enhanced_size_bytes"));
                }

                return responseBody;
            }

            log.warn("Preprocessing service returned unsuccessful response: HTTP {}", response.getStatusCode());
            return null;
        } catch (Exception e) {
            log.error("Error calling preprocessing service: {}", e.getMessage(), e);
            return null;
        }
    }

    @SuppressWarnings("unused")
    private Map<String, Object> callPreprocessingServiceFallback(File imageFile, String animalType, Exception ex) {
        log.warn("AI preprocessing service unavailable for {}: {}. Skipping AI classification.",
                animalType, ex.getMessage());
        return null;
    }
}