package org.example.backend.service;

import org.example.backend.event.AnimalDetectedEvent;
import org.example.backend.event.ClassificationResultEvent;
import org.example.backend.model.AnimalDetection;
import org.example.backend.repository.AnimalDetectionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClassificationResultProcessor Unit Tests")
class ClassificationResultProcessorTest {

    @Mock
    private AnimalDetectionRepository animalDetectionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ClassificationResultProcessor processor;

    @Captor
    private ArgumentCaptor<AnimalDetection> detectionCaptor;

    @Test
    @DisplayName("Should register and retrieve job")
    void shouldRegisterAndRetrieveJob() {
        processor.registerJob("job-123", 456L);
        assertThat(processor.getDetectionIdForJob("job-123")).isEqualTo(456L);
        assertThat(processor.getPendingJobCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should handle missing detection gracefully")
    void shouldHandleMissingDetection() {
        processor.registerJob("job-123", 456L);
        when(animalDetectionRepository.findById(456L)).thenReturn(Optional.empty());

        ClassificationResultEvent event = new ClassificationResultEvent(this, "job-123", 456L, true, Map.of());
        processor.onClassificationResult(event);

        verify(animalDetectionRepository, never()).save(any());
        assertThat(processor.getPendingJobCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle failed classification event")
    void shouldHandleFailedClassification() {
        AnimalDetection detection = new AnimalDetection();
        detection.setId(456L);

        processor.registerJob("job-failed", 456L);
        when(animalDetectionRepository.findById(456L)).thenReturn(Optional.of(detection));
        when(animalDetectionRepository.save(any(AnimalDetection.class))).thenReturn(detection);

        ClassificationResultEvent event = new ClassificationResultEvent(this, "job-failed", 456L, false,
                Map.of("error", "API timeout"));
        processor.onClassificationResult(event);

        verify(animalDetectionRepository).save(detectionCaptor.capture());
        AnimalDetection saved = detectionCaptor.getValue();

        assertThat(saved.getSpecies()).isEqualTo("Classification failed");
        assertThat(saved.getAlternateSpecies()).isEqualTo("API timeout");
        assertThat(saved.isAiProcessed()).isTrue();

        verify(eventPublisher, never()).publishEvent(any(AnimalDetectedEvent.class));
    }

    @Test
    @DisplayName("Should handle success with missing classification data")
    void shouldHandleMissingClassificationData() {
        AnimalDetection detection = new AnimalDetection();
        detection.setId(456L);

        processor.registerJob("job-no-data", 456L);
        when(animalDetectionRepository.findById(456L)).thenReturn(Optional.of(detection));
        when(animalDetectionRepository.save(any(AnimalDetection.class))).thenReturn(detection);

        ClassificationResultEvent event = new ClassificationResultEvent(this, "job-no-data", 456L, true, Map.of());
        processor.onClassificationResult(event);

        verify(animalDetectionRepository).save(detectionCaptor.capture());
        AnimalDetection saved = detectionCaptor.getValue();

        assertThat(saved.isAiProcessed()).isTrue();
        verify(eventPublisher, never()).publishEvent(any(AnimalDetectedEvent.class)); // because species is null
                                                                                      // initially
    }

    @Test
    @DisplayName("Should handle no animal detected")
    void shouldHandleNoAnimalDetected() {
        AnimalDetection detection = new AnimalDetection();
        detection.setId(456L);

        processor.registerJob("job-no-animal", 456L);
        when(animalDetectionRepository.findById(456L)).thenReturn(Optional.of(detection));
        when(animalDetectionRepository.save(any(AnimalDetection.class))).thenReturn(detection);

        Map<String, Object> result = Map.of(
                "classification", Map.of(),
                "detection", Map.of("bird_detected", false, "animal_detected", false));

        ClassificationResultEvent event = new ClassificationResultEvent(this, "job-no-animal", 456L, true, result);
        processor.onClassificationResult(event);

        verify(animalDetectionRepository).save(detectionCaptor.capture());
        AnimalDetection saved = detectionCaptor.getValue();

        assertThat(saved.getSpecies()).isEqualTo("No animal detected");
        assertThat(saved.getConfidence()).isEqualTo(0.0f);
        assertThat(saved.isAiProcessed()).isTrue();

        verify(eventPublisher).publishEvent(any(AnimalDetectedEvent.class));
    }

    @Test
    @DisplayName("Should handle successful classification with taxonomic names")
    void shouldHandleSuccessfulClassification() {
        AnimalDetection detection = new AnimalDetection();
        detection.setId(456L);

        processor.registerJob("job-success", 456L);
        when(animalDetectionRepository.findById(456L)).thenReturn(Optional.of(detection));
        when(animalDetectionRepository.save(any(AnimalDetection.class))).thenReturn(detection);

        Map<String, Object> classification = Map.of(
                "top_species", "uuid;mammalia;carnivora;canidae;canis;lupus;wolf",
                "top_confidence", 0.95,
                "predictions", List.of(
                        Map.of("species", "uuid;mammalia;carnivora;canidae;canis;lupus;wolf", "confidence", 0.95),
                        Map.of("species", "uuid;mammalia;carnivora;canidae;canis;latrans;coyote", "confidence", 0.04)));

        Map<String, Object> result = Map.of(
                "classification", classification,
                "detection", Map.of("animal_detected", true));

        ClassificationResultEvent event = new ClassificationResultEvent(this, "job-success", 456L, true, result);
        processor.onClassificationResult(event);

        verify(animalDetectionRepository).save(detectionCaptor.capture());
        AnimalDetection saved = detectionCaptor.getValue();

        assertThat(saved.getSpecies()).isEqualTo("wolf");
        assertThat(saved.getConfidence()).isEqualTo(0.95f);
        assertThat(saved.getAlternateSpecies()).contains("coyote (4.0%)");
        assertThat(saved.isAiProcessed()).isTrue();

        verify(eventPublisher).publishEvent(any(AnimalDetectedEvent.class));
    }

    @Test
    @DisplayName("Should extract species name if common name is empty")
    void shouldExtractSpeciesNameIfCommonNameEmpty() {
        AnimalDetection detection = new AnimalDetection();
        detection.setId(456L);

        processor.registerJob("job-no-common", 456L);
        when(animalDetectionRepository.findById(456L)).thenReturn(Optional.of(detection));
        when(animalDetectionRepository.save(any(AnimalDetection.class))).thenReturn(detection);

        Map<String, Object> classification = Map.of(
                "top_species", "uuid;mammalia;carnivora;canidae;canis;lupus;", // Empty common name at index 6
                "top_confidence", 0.90);

        Map<String, Object> result = Map.of(
                "classification", classification,
                "detection", Map.of("bird_detected", true));

        ClassificationResultEvent event = new ClassificationResultEvent(this, "job-no-common", 456L, true, result);
        processor.onClassificationResult(event);

        verify(animalDetectionRepository).save(detectionCaptor.capture());
        assertThat(detectionCaptor.getValue().getSpecies()).isEqualTo("lupus"); // Should fall back to index 5
    }
}
