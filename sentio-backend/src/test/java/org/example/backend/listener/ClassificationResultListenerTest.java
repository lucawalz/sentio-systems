package org.example.backend.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.backend.event.ClassificationResultEvent;
import org.example.backend.service.ClassificationResultProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClassificationResultListener Unit Tests")
class ClassificationResultListenerTest {

        @Mock
        private ObjectMapper objectMapper;

        @Mock
        private ApplicationEventPublisher eventPublisher;

        @Mock
        private ClassificationResultProcessor resultProcessor;

        @InjectMocks
        private ClassificationResultListener listener;

        @Mock
        private org.springframework.data.redis.connection.RedisConnectionFactory connectionFactory;

        private static final String JOB_ID = "job-123-456";
        private static final Long DETECTION_ID = 789L;

        @Nested
        @DisplayName("onMessage Tests - Successful Processing")
        class OnMessageSuccessTests {

                @Test
                @DisplayName("Should publish event when valid result is received with success=true")
                void shouldPublishEventForSuccessfulResult() throws Exception {
                        // Given
                        Map<String, Object> resultMap = new HashMap<>();
                        resultMap.put("job_id", JOB_ID);
                        resultMap.put("success", true);
                        resultMap.put("species", "European Robin");
                        resultMap.put("confidence", 0.95);

                        String json = "{\"job_id\":\"" + JOB_ID + "\",\"success\":true}";
                        Message message = new DefaultMessage("test".getBytes(), json.getBytes());

                        when(objectMapper.readValue(eq(json), any(TypeReference.class)))
                                        .thenReturn(resultMap);
                        when(resultProcessor.getDetectionIdForJob(JOB_ID)).thenReturn(DETECTION_ID);

                        // When
                        listener.onMessage(message, null);

                        // Then
                        ArgumentCaptor<ClassificationResultEvent> eventCaptor = ArgumentCaptor
                                        .forClass(ClassificationResultEvent.class);
                        verify(eventPublisher).publishEvent(eventCaptor.capture());

                        ClassificationResultEvent capturedEvent = eventCaptor.getValue();
                        assertThat(capturedEvent.getJobId()).isEqualTo(JOB_ID);
                        assertThat(capturedEvent.getDetectionId()).isEqualTo(DETECTION_ID);
                        assertThat(capturedEvent.isSuccess()).isTrue();
                        assertThat(capturedEvent.getResult()).isEqualTo(resultMap);
                }

                @Test
                @DisplayName("Should publish event when valid result is received with success=false")
                void shouldPublishEventForFailedResult() throws Exception {
                        // Given
                        Map<String, Object> resultMap = new HashMap<>();
                        resultMap.put("job_id", JOB_ID);
                        resultMap.put("success", false);
                        resultMap.put("error", "Classification failed");

                        String json = "{\"job_id\":\"" + JOB_ID + "\",\"success\":false}";
                        Message message = new DefaultMessage("test".getBytes(), json.getBytes());

                        when(objectMapper.readValue(eq(json), any(TypeReference.class)))
                                        .thenReturn(resultMap);
                        when(resultProcessor.getDetectionIdForJob(JOB_ID)).thenReturn(DETECTION_ID);

                        // When
                        listener.onMessage(message, null);

                        // Then
                        ArgumentCaptor<ClassificationResultEvent> eventCaptor = ArgumentCaptor
                                        .forClass(ClassificationResultEvent.class);
                        verify(eventPublisher).publishEvent(eventCaptor.capture());

                        ClassificationResultEvent capturedEvent = eventCaptor.getValue();
                        assertThat(capturedEvent.getJobId()).isEqualTo(JOB_ID);
                        assertThat(capturedEvent.getDetectionId()).isEqualTo(DETECTION_ID);
                        assertThat(capturedEvent.isSuccess()).isFalse();
                }

                @Test
                @DisplayName("Should handle success=null as false")
                void shouldHandleNullSuccessAsFalse() throws Exception {
                        // Given
                        Map<String, Object> resultMap = new HashMap<>();
                        resultMap.put("job_id", JOB_ID);
                        resultMap.put("success", null);

                        String json = "{\"job_id\":\"" + JOB_ID + "\",\"success\":null}";
                        Message message = new DefaultMessage("test".getBytes(), json.getBytes());

                        when(objectMapper.readValue(eq(json), any(TypeReference.class)))
                                        .thenReturn(resultMap);
                        when(resultProcessor.getDetectionIdForJob(JOB_ID)).thenReturn(DETECTION_ID);

                        // When
                        listener.onMessage(message, null);

                        // Then
                        ArgumentCaptor<ClassificationResultEvent> eventCaptor = ArgumentCaptor
                                        .forClass(ClassificationResultEvent.class);
                        verify(eventPublisher).publishEvent(eventCaptor.capture());

                        assertThat(eventCaptor.getValue().isSuccess()).isFalse();
                }

                @Test
                @DisplayName("Should handle result with additional metadata")
                void shouldHandleResultWithMetadata() throws Exception {
                        // Given
                        Map<String, Object> resultMap = new HashMap<>();
                        resultMap.put("job_id", JOB_ID);
                        resultMap.put("success", true);
                        resultMap.put("species", "Blue Jay");
                        resultMap.put("confidence", 0.88);
                        resultMap.put("processing_time", 1234L);
                        resultMap.put("model_version", "v2.1");

                        String json = "{\"job_id\":\"" + JOB_ID + "\",\"success\":true}";
                        Message message = new DefaultMessage("test".getBytes(), json.getBytes());

                        when(objectMapper.readValue(eq(json), any(TypeReference.class)))
                                        .thenReturn(resultMap);
                        when(resultProcessor.getDetectionIdForJob(JOB_ID)).thenReturn(DETECTION_ID);

                        // When
                        listener.onMessage(message, null);

                        // Then
                        ArgumentCaptor<ClassificationResultEvent> eventCaptor = ArgumentCaptor
                                        .forClass(ClassificationResultEvent.class);
                        verify(eventPublisher).publishEvent(eventCaptor.capture());

                        Map<String, Object> capturedResult = eventCaptor.getValue().getResult();
                        assertThat(capturedResult).containsEntry("species", "Blue Jay");
                        assertThat(capturedResult).containsEntry("confidence", 0.88);
                        assertThat(capturedResult).containsEntry("processing_time", 1234L);
                        assertThat(capturedResult).containsEntry("model_version", "v2.1");
                }
        }

        @Nested
        @DisplayName("onMessage Tests - Invalid Input Handling")
        class OnMessageInvalidInputTests {

                @Test
                @DisplayName("Should not publish event when job_id is missing")
                void shouldNotPublishEventWhenJobIdMissing() throws Exception {
                        // Given
                        Map<String, Object> resultMap = new HashMap<>();
                        resultMap.put("success", true);

                        String json = "{\"success\":true}";
                        Message message = new DefaultMessage("test".getBytes(), json.getBytes());

                        when(objectMapper.readValue(eq(json), any(TypeReference.class)))
                                        .thenReturn(resultMap);

                        // When
                        listener.onMessage(message, null);

                        // Then
                        verify(eventPublisher, never()).publishEvent(any());
                        verify(resultProcessor, never()).getDetectionIdForJob(anyString());
                }

                @Test
                @DisplayName("Should not publish event when job_id is null")
                void shouldNotPublishEventWhenJobIdNull() throws Exception {
                        // Given
                        Map<String, Object> resultMap = new HashMap<>();
                        resultMap.put("job_id", null);
                        resultMap.put("success", true);

                        String json = "{\"job_id\":null,\"success\":true}";
                        Message message = new DefaultMessage("test".getBytes(), json.getBytes());

                        when(objectMapper.readValue(eq(json), any(TypeReference.class)))
                                        .thenReturn(resultMap);

                        // When
                        listener.onMessage(message, null);

                        // Then
                        verify(eventPublisher, never()).publishEvent(any());
                }

                @Test
                @DisplayName("Should not publish event when detection ID is not found")
                void shouldNotPublishEventWhenDetectionIdNotFound() throws Exception {
                        // Given
                        Map<String, Object> resultMap = new HashMap<>();
                        resultMap.put("job_id", JOB_ID);
                        resultMap.put("success", true);

                        String json = "{\"job_id\":\"" + JOB_ID + "\",\"success\":true}";
                        Message message = new DefaultMessage("test".getBytes(), json.getBytes());

                        when(objectMapper.readValue(eq(json), any(TypeReference.class)))
                                        .thenReturn(resultMap);
                        when(resultProcessor.getDetectionIdForJob(JOB_ID)).thenReturn(null);

                        // When
                        listener.onMessage(message, null);

                        // Then
                        verify(resultProcessor).getDetectionIdForJob(JOB_ID);
                        verify(eventPublisher, never()).publishEvent(any());
                }

                @Test
                @DisplayName("Should handle empty message body gracefully")
                void shouldHandleEmptyMessageBody() throws Exception {
                        // Given
                        String json = "";
                        Message message = new DefaultMessage("test".getBytes(), json.getBytes());

                        when(objectMapper.readValue(eq(json), any(TypeReference.class)))
                                        .thenThrow(new RuntimeException("Empty JSON"));

                        // When
                        listener.onMessage(message, null);

                        // Then
                        verify(eventPublisher, never()).publishEvent(any());
                }

                @Test
                @DisplayName("Should handle malformed JSON gracefully")
                void shouldHandleMalformedJson() throws Exception {
                        // Given
                        String json = "{invalid json}";
                        Message message = new DefaultMessage("test".getBytes(), json.getBytes());

                        when(objectMapper.readValue(eq(json), any(TypeReference.class)))
                                        .thenThrow(new RuntimeException("Invalid JSON"));

                        // When
                        listener.onMessage(message, null);

                        // Then
                        verify(eventPublisher, never()).publishEvent(any());
                }

                @Test
                @DisplayName("Should handle null message body gracefully")
                void shouldHandleNullMessage() {
                        // Given
                        Message message = mock(Message.class);
                        when(message.getBody()).thenReturn(null);

                        // When
                        listener.onMessage(message, null);

                        // Then
                        verify(eventPublisher, never()).publishEvent(any());
                }
        }

        @Nested
        @DisplayName("onMessage Tests - Error Handling")
        class OnMessageErrorHandlingTests {

                @Test
                @DisplayName("Should handle ObjectMapper exception gracefully")
                void shouldHandleObjectMapperException() throws Exception {
                        // Given
                        String json = "{\"job_id\":\"" + JOB_ID + "\"}";
                        Message message = new DefaultMessage("test".getBytes(), json.getBytes());

                        when(objectMapper.readValue(eq(json), any(TypeReference.class)))
                                        .thenThrow(new RuntimeException("Deserialization error"));

                        // When & Then - should not throw
                        listener.onMessage(message, null);

                        verify(eventPublisher, never()).publishEvent(any());
                }

                @Test
                @DisplayName("Should handle resultProcessor exception gracefully")
                void shouldHandleResultProcessorException() throws Exception {
                        // Given
                        Map<String, Object> resultMap = new HashMap<>();
                        resultMap.put("job_id", JOB_ID);
                        resultMap.put("success", true);

                        String json = "{\"job_id\":\"" + JOB_ID + "\"}";
                        Message message = new DefaultMessage("test".getBytes(), json.getBytes());

                        when(objectMapper.readValue(eq(json), any(TypeReference.class)))
                                        .thenReturn(resultMap);
                        when(resultProcessor.getDetectionIdForJob(JOB_ID))
                                        .thenThrow(new RuntimeException("Processor error"));

                        // When & Then - should not throw
                        listener.onMessage(message, null);

                        verify(eventPublisher, never()).publishEvent(any());
                }

                @Test
                @DisplayName("Should handle eventPublisher exception gracefully")
                void shouldHandleEventPublisherException() throws Exception {
                        // Given
                        Map<String, Object> resultMap = new HashMap<>();
                        resultMap.put("job_id", JOB_ID);
                        resultMap.put("success", true);

                        String json = "{\"job_id\":\"" + JOB_ID + "\"}";
                        Message message = new DefaultMessage("test".getBytes(), json.getBytes());

                        when(objectMapper.readValue(eq(json), any(TypeReference.class)))
                                        .thenReturn(resultMap);
                        when(resultProcessor.getDetectionIdForJob(JOB_ID)).thenReturn(DETECTION_ID);
                        doThrow(new RuntimeException("Event publish error"))
                                        .when(eventPublisher).publishEvent(any());

                        // When & Then - should not throw
                        listener.onMessage(message, null);

                        verify(eventPublisher).publishEvent(any(ClassificationResultEvent.class));
                }
        }

        @Nested
        @DisplayName("Edge Cases and Special Scenarios")
        class EdgeCasesTests {

                @Test
                @DisplayName("Should handle empty job_id string")
                void shouldHandleEmptyJobId() throws Exception {
                        // Given
                        Map<String, Object> resultMap = new HashMap<>();
                        resultMap.put("job_id", "");
                        resultMap.put("success", true);

                        String json = "{\"job_id\":\"\",\"success\":true}";
                        Message message = new DefaultMessage("test".getBytes(), json.getBytes());

                        when(objectMapper.readValue(eq(json), any(TypeReference.class)))
                                        .thenReturn(resultMap);
                        when(resultProcessor.getDetectionIdForJob("")).thenReturn(null);

                        // When
                        listener.onMessage(message, null);

                        // Then
                        verify(resultProcessor).getDetectionIdForJob("");
                        verify(eventPublisher, never()).publishEvent(any());
                }

                @Test
                @DisplayName("Should handle very long job_id")
                void shouldHandleVeryLongJobId() throws Exception {
                        // Given
                        String longJobId = "job-" + "x".repeat(1000);
                        Map<String, Object> resultMap = new HashMap<>();
                        resultMap.put("job_id", longJobId);
                        resultMap.put("success", true);

                        String json = "{\"job_id\":\"" + longJobId + "\",\"success\":true}";
                        Message message = new DefaultMessage("test".getBytes(), json.getBytes());

                        when(objectMapper.readValue(eq(json), any(TypeReference.class)))
                                        .thenReturn(resultMap);
                        when(resultProcessor.getDetectionIdForJob(longJobId)).thenReturn(DETECTION_ID);

                        // When
                        listener.onMessage(message, null);

                        // Then
                        verify(resultProcessor).getDetectionIdForJob(longJobId);
                        verify(eventPublisher).publishEvent(any(ClassificationResultEvent.class));
                }

                @Test
                @DisplayName("Should handle special characters in job_id")
                void shouldHandleSpecialCharactersInJobId() throws Exception {
                        // Given
                        String specialJobId = "job-123-@#$%^&*()";
                        Map<String, Object> resultMap = new HashMap<>();
                        resultMap.put("job_id", specialJobId);
                        resultMap.put("success", true);

                        String json = "{\"job_id\":\"" + specialJobId + "\",\"success\":true}";
                        Message message = new DefaultMessage("test".getBytes(), json.getBytes());

                        when(objectMapper.readValue(eq(json), any(TypeReference.class)))
                                        .thenReturn(resultMap);
                        when(resultProcessor.getDetectionIdForJob(specialJobId)).thenReturn(DETECTION_ID);

                        // When
                        listener.onMessage(message, null);

                        // Then
                        verify(resultProcessor).getDetectionIdForJob(specialJobId);
                        verify(eventPublisher).publishEvent(any(ClassificationResultEvent.class));
                }

                @Test
                @DisplayName("Should handle result with empty map")
                void shouldHandleEmptyResultMap() throws Exception {
                        // Given
                        Map<String, Object> resultMap = new HashMap<>();
                        resultMap.put("job_id", JOB_ID);
                        resultMap.put("success", true);

                        String json = "{\"job_id\":\"" + JOB_ID + "\",\"success\":true}";
                        Message message = new DefaultMessage("test".getBytes(), json.getBytes());

                        when(objectMapper.readValue(eq(json), any(TypeReference.class)))
                                        .thenReturn(resultMap);
                        when(resultProcessor.getDetectionIdForJob(JOB_ID)).thenReturn(DETECTION_ID);

                        // When
                        listener.onMessage(message, null);

                        // Then
                        ArgumentCaptor<ClassificationResultEvent> eventCaptor = ArgumentCaptor
                                        .forClass(ClassificationResultEvent.class);
                        verify(eventPublisher).publishEvent(eventCaptor.capture());

                        assertThat(eventCaptor.getValue().getResult()).isEqualTo(resultMap);
                }

                @Test
                @DisplayName("Should handle Unicode characters in result")
                void shouldHandleUnicodeCharacters() throws Exception {
                        // Given
                        Map<String, Object> resultMap = new HashMap<>();
                        resultMap.put("job_id", JOB_ID);
                        resultMap.put("success", true);
                        resultMap.put("species", "鳥類 Bird 🐦");

                        String json = "{\"job_id\":\"" + JOB_ID + "\",\"success\":true}";
                        Message message = new DefaultMessage("test".getBytes(), json.getBytes());

                        when(objectMapper.readValue(eq(json), any(TypeReference.class)))
                                        .thenReturn(resultMap);
                        when(resultProcessor.getDetectionIdForJob(JOB_ID)).thenReturn(DETECTION_ID);

                        // When
                        listener.onMessage(message, null);

                        // Then
                        ArgumentCaptor<ClassificationResultEvent> eventCaptor = ArgumentCaptor
                                        .forClass(ClassificationResultEvent.class);
                        verify(eventPublisher).publishEvent(eventCaptor.capture());

                        assertThat(eventCaptor.getValue().getResult().get("species"))
                                        .isEqualTo("鳥類 Bird 🐦");
                }

                @Test
                @DisplayName("Should handle multiple rapid messages")
                void shouldHandleMultipleRapidMessages() throws Exception {
                        // Given
                        Map<String, Object> resultMap1 = new HashMap<>();
                        resultMap1.put("job_id", "job-1");
                        resultMap1.put("success", true);

                        Map<String, Object> resultMap2 = new HashMap<>();
                        resultMap2.put("job_id", "job-2");
                        resultMap2.put("success", true);

                        String json1 = "{\"job_id\":\"job-1\",\"success\":true}";
                        String json2 = "{\"job_id\":\"job-2\",\"success\":true}";

                        Message message1 = new DefaultMessage("test".getBytes(), json1.getBytes());
                        Message message2 = new DefaultMessage("test".getBytes(), json2.getBytes());

                        when(objectMapper.readValue(eq(json1), any(TypeReference.class)))
                                        .thenReturn(resultMap1);
                        when(objectMapper.readValue(eq(json2), any(TypeReference.class)))
                                        .thenReturn(resultMap2);
                        when(resultProcessor.getDetectionIdForJob("job-1")).thenReturn(1L);
                        when(resultProcessor.getDetectionIdForJob("job-2")).thenReturn(2L);

                        // When
                        listener.onMessage(message1, null);
                        listener.onMessage(message2, null);

                        // Then
                        verify(eventPublisher, times(2)).publishEvent(any(ClassificationResultEvent.class));
                }

                @Test
                @DisplayName("Should handle result with nested objects")
                void shouldHandleNestedObjects() throws Exception {
                        // Given
                        Map<String, Object> nestedData = new HashMap<>();
                        nestedData.put("model", "v2.0");
                        nestedData.put("accuracy", 0.95);

                        Map<String, Object> resultMap = new HashMap<>();
                        resultMap.put("job_id", JOB_ID);
                        resultMap.put("success", true);
                        resultMap.put("metadata", nestedData);

                        String json = "{\"job_id\":\"" + JOB_ID + "\",\"success\":true}";
                        Message message = new DefaultMessage("test".getBytes(), json.getBytes());

                        when(objectMapper.readValue(eq(json), any(TypeReference.class)))
                                        .thenReturn(resultMap);
                        when(resultProcessor.getDetectionIdForJob(JOB_ID)).thenReturn(DETECTION_ID);

                        // When
                        listener.onMessage(message, null);

                        // Then
                        ArgumentCaptor<ClassificationResultEvent> eventCaptor = ArgumentCaptor
                                        .forClass(ClassificationResultEvent.class);
                        verify(eventPublisher).publishEvent(eventCaptor.capture());

                        assertThat(eventCaptor.getValue().getResult().get("metadata"))
                                        .isEqualTo(nestedData);
                }
        }
}
