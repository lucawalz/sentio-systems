package org.example.backend.controller;

import org.example.backend.model.WorkflowResult;
import org.example.backend.model.WorkflowType;
import org.example.backend.service.N8nWorkflowTriggerService;
import org.example.backend.service.WorkflowService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = WorkflowController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class
})
@Import(WorkflowControllerTest.TestBeans.class)
@org.springframework.test.context.TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
class WorkflowControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    WorkflowService workflowService;

    @Autowired
    N8nWorkflowTriggerService n8nWorkflowTriggerService;

    @AfterEach
    void clearMockInvocations() {
        clearInvocations(workflowService, n8nWorkflowTriggerService);
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        WorkflowService workflowService() {
            return mock(WorkflowService.class);
        }

        @Bean
        N8nWorkflowTriggerService n8nWorkflowTriggerService() {
            return mock(N8nWorkflowTriggerService.class);
        }
    }

    @Test
    void getCurrentResult_returns200_whenPresent() throws Exception {
        WorkflowResult result = new WorkflowResult();
        result.setId(1L);
        result.setAnalysisText("Hello");
        result.setWorkflowType(WorkflowType.WEATHER_SUMMARY);
        result.setTimestamp(LocalDateTime.of(2025, 12, 18, 10, 0));

        when(workflowService.getCurrentResult()).thenReturn(Optional.of(result));

        mockMvc.perform(get("/api/workflow/current"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.analysisText").value("Hello"))
                .andExpect(jsonPath("$.workflowType").value("WEATHER_SUMMARY"))
                .andExpect(jsonPath("$.timestamp").value("2025-12-18T10:00:00"));

        verify(workflowService, times(1)).getCurrentResult();
        verifyNoMoreInteractions(workflowService);
    }

    @Test
    void getCurrentResult_returns204_whenEmpty() throws Exception {
        when(workflowService.getCurrentResult()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/workflow/current"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(workflowService).getCurrentResult();
        verifyNoMoreInteractions(workflowService);
    }

    @Test
    void getCurrentSummary_returns200_whenPresent() throws Exception {
        WorkflowResult summary = new WorkflowResult();
        summary.setId(1L);
        summary.setAnalysisText("Summary text");
        summary.setWorkflowType(WorkflowType.WEATHER_SUMMARY);
        summary.setTimestamp(LocalDateTime.of(2025, 12, 18, 10, 0));

        when(workflowService.getCurrentSummary()).thenReturn(Optional.of(summary));

        mockMvc.perform(get("/api/workflow/summaries/current"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.workflowType").value("WEATHER_SUMMARY"));

        verify(workflowService).getCurrentSummary();
        verifyNoMoreInteractions(workflowService);
    }

    @Test
    void getRecentResults_returns200_andArray() throws Exception {
        WorkflowResult a = new WorkflowResult();
        a.setId(1L);
        a.setAnalysisText("A");
        a.setWorkflowType(WorkflowType.WEATHER_SUMMARY);
        a.setTimestamp(LocalDateTime.of(2025, 12, 18, 9, 0));

        WorkflowResult b = new WorkflowResult();
        b.setId(2L);
        b.setAnalysisText("B");
        b.setWorkflowType(WorkflowType.AGENT_RESPONSE);
        b.setTimestamp(LocalDateTime.of(2025, 12, 18, 8, 0));

        when(workflowService.getRecentResults()).thenReturn(List.of(a, b));

        mockMvc.perform(get("/api/workflow/recent"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].analysisText").value("A"))
                .andExpect(jsonPath("$[1].analysisText").value("B"));

        verify(workflowService, times(1)).getRecentResults();
        verifyNoMoreInteractions(workflowService);
    }

    @Test
    void getRecentSummaries_returns200_andArray() throws Exception {
        WorkflowResult summary = new WorkflowResult();
        summary.setId(1L);
        summary.setAnalysisText("Summary");
        summary.setWorkflowType(WorkflowType.WEATHER_SUMMARY);

        when(workflowService.getRecentSummaries()).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/workflow/summaries/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].workflowType").value("WEATHER_SUMMARY"));

        verify(workflowService).getRecentSummaries();
        verifyNoMoreInteractions(workflowService);
    }

    @Test
    void createWorkflowResult_setsDefaultsWhenNull() throws Exception {
        when(workflowService.saveWorkflowResult(any(WorkflowResult.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/workflow")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        ArgumentCaptor<WorkflowResult> captor = ArgumentCaptor.forClass(WorkflowResult.class);
        verify(workflowService).saveWorkflowResult(captor.capture());

        assertThat(captor.getValue().getTimestamp()).isNotNull();
        assertThat(captor.getValue().getWorkflowType()).isEqualTo(WorkflowType.WEATHER_SUMMARY);

        verifyNoMoreInteractions(workflowService);
    }

    @Test
    void createWorkflowResult_preservesProvidedType() throws Exception {
        when(workflowService.saveWorkflowResult(any(WorkflowResult.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/workflow")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"workflowType\": \"AGENT_RESPONSE\", \"analysisText\": \"Agent reply\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<WorkflowResult> captor = ArgumentCaptor.forClass(WorkflowResult.class);
        verify(workflowService).saveWorkflowResult(captor.capture());

        assertThat(captor.getValue().getWorkflowType()).isEqualTo(WorkflowType.AGENT_RESPONSE);
        assertThat(captor.getValue().getAnalysisText()).isEqualTo("Agent reply");
    }

    @Test
    void cleanupOldResults_callsService_andReturns200() throws Exception {
        doNothing().when(workflowService).cleanupOldResults();

        mockMvc.perform(post("/api/workflow/cleanup"))
                .andExpect(status().isOk())
                .andExpect(content().string("Cleanup completed successfully"));

        verify(workflowService).cleanupOldResults();
        verifyNoMoreInteractions(workflowService);
    }

    @Test
    void getMyWeatherSummary_returns200_whenPresent() throws Exception {
        try (org.mockito.MockedStatic<org.example.backend.util.SecurityUtils> mocked = mockStatic(
                org.example.backend.util.SecurityUtils.class)) {
            mocked.when(org.example.backend.util.SecurityUtils::getCurrentUserId).thenReturn("user-1");
            WorkflowResult summary = new WorkflowResult();
            summary.setAnalysisText("User weather summary");
            when(workflowService.getCurrentWeatherSummary("user-1")).thenReturn(Optional.of(summary));

            mockMvc.perform(get("/api/workflow/me/weather"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.analysisText").value("User weather summary"));
        }
    }

    @Test
    void getMyWeatherSummary_returns204_whenEmpty() throws Exception {
        try (org.mockito.MockedStatic<org.example.backend.util.SecurityUtils> mocked = mockStatic(
                org.example.backend.util.SecurityUtils.class)) {
            mocked.when(org.example.backend.util.SecurityUtils::getCurrentUserId).thenReturn("user-1");
            when(workflowService.getCurrentWeatherSummary("user-1")).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/workflow/me/weather"))
                    .andExpect(status().isNoContent());
        }
    }

    @Test
    void getMySightingsSummary_returns200_whenPresent() throws Exception {
        try (org.mockito.MockedStatic<org.example.backend.util.SecurityUtils> mocked = mockStatic(
                org.example.backend.util.SecurityUtils.class)) {
            mocked.when(org.example.backend.util.SecurityUtils::getCurrentUserId).thenReturn("user-2");
            WorkflowResult summary = new WorkflowResult();
            summary.setAnalysisText("Sightings");
            when(workflowService.getCurrentSightingsSummary("user-2")).thenReturn(Optional.of(summary));

            mockMvc.perform(get("/api/workflow/me/sightings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.analysisText").value("Sightings"));
        }
    }

    @Test
    void getMySightingsSummary_returns204_whenEmpty() throws Exception {
        try (org.mockito.MockedStatic<org.example.backend.util.SecurityUtils> mocked = mockStatic(
                org.example.backend.util.SecurityUtils.class)) {
            mocked.when(org.example.backend.util.SecurityUtils::getCurrentUserId).thenReturn("user-2");
            when(workflowService.getCurrentSightingsSummary("user-2")).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/workflow/me/sightings"))
                    .andExpect(status().isNoContent());
        }
    }

    @Test
    void getMyRecentResults_returns200() throws Exception {
        try (org.mockito.MockedStatic<org.example.backend.util.SecurityUtils> mocked = mockStatic(
                org.example.backend.util.SecurityUtils.class)) {
            mocked.when(org.example.backend.util.SecurityUtils::getCurrentUserId).thenReturn("user-3");
            WorkflowResult r1 = new WorkflowResult();
            r1.setAnalysisText("Recent Result");
            when(workflowService.getUserRecentResults("user-3")).thenReturn(List.of(r1));

            mockMvc.perform(get("/api/workflow/me/recent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].analysisText").value("Recent Result"));
        }
    }

    @Test
    void createUserWorkflowResult_returns200() throws Exception {
        try (org.mockito.MockedStatic<org.example.backend.util.SecurityUtils> mocked = mockStatic(
                org.example.backend.util.SecurityUtils.class)) {
            mocked.when(org.example.backend.util.SecurityUtils::getCurrentUserId).thenReturn("user-4");
            when(workflowService.saveUserWorkflowResult(eq("user-4"), any(WorkflowResult.class)))
                    .thenAnswer(inv -> inv.getArgument(1));

            mockMvc.perform(post("/api/workflow/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"analysisText\": \"Save me\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.analysisText").value("Save me"))
                    .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }
    }

    @Test
    void generateWeatherSummary_returns202_whenTrue() throws Exception {
        try (org.mockito.MockedStatic<org.example.backend.util.SecurityUtils> mocked = mockStatic(
                org.example.backend.util.SecurityUtils.class)) {
            mocked.when(org.example.backend.util.SecurityUtils::getCurrentUserId).thenReturn("user-5");
            when(n8nWorkflowTriggerService.triggerWeatherSummary("user-5")).thenReturn(true);

            mockMvc.perform(post("/api/workflow/generate/weather"))
                    .andExpect(status().isAccepted())
                    .andExpect(content().string("Weather summary generation triggered"));
        }
    }

    @Test
    void generateWeatherSummary_returns500_whenFalse() throws Exception {
        try (org.mockito.MockedStatic<org.example.backend.util.SecurityUtils> mocked = mockStatic(
                org.example.backend.util.SecurityUtils.class)) {
            mocked.when(org.example.backend.util.SecurityUtils::getCurrentUserId).thenReturn("user-5");
            when(n8nWorkflowTriggerService.triggerWeatherSummary("user-5")).thenReturn(false);

            mockMvc.perform(post("/api/workflow/generate/weather"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string("Failed to trigger workflow"));
        }
    }

    @Test
    void generateSightingsSummary_returns202_whenTrue() throws Exception {
        try (org.mockito.MockedStatic<org.example.backend.util.SecurityUtils> mocked = mockStatic(
                org.example.backend.util.SecurityUtils.class)) {
            mocked.when(org.example.backend.util.SecurityUtils::getCurrentUserId).thenReturn("user-6");
            when(n8nWorkflowTriggerService.triggerSightingsSummary("user-6")).thenReturn(true);

            mockMvc.perform(post("/api/workflow/generate/sightings"))
                    .andExpect(status().isAccepted())
                    .andExpect(content().string("Sightings summary generation triggered"));
        }
    }

    @Test
    void generateSightingsSummary_returns500_whenFalse() throws Exception {
        try (org.mockito.MockedStatic<org.example.backend.util.SecurityUtils> mocked = mockStatic(
                org.example.backend.util.SecurityUtils.class)) {
            mocked.when(org.example.backend.util.SecurityUtils::getCurrentUserId).thenReturn("user-6");
            when(n8nWorkflowTriggerService.triggerSightingsSummary("user-6")).thenReturn(false);

            mockMvc.perform(post("/api/workflow/generate/sightings"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string("Failed to trigger workflow"));
        }
    }

    @Test
    void askAgent_returns200_whenResponseNotNull() throws Exception {
        try (org.mockito.MockedStatic<org.example.backend.util.SecurityUtils> mocked = mockStatic(
                org.example.backend.util.SecurityUtils.class)) {
            mocked.when(org.example.backend.util.SecurityUtils::getCurrentUserId).thenReturn("user-7");
            when(n8nWorkflowTriggerService.triggerAiAgent("user-7", "What is the weather?")).thenReturn("It is sunny");

            mockMvc.perform(post("/api/workflow/agent/ask")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"query\": \"What is the weather?\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.response").value("It is sunny"))
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Test
    void askAgent_returns500_whenResponseNull() throws Exception {
        try (org.mockito.MockedStatic<org.example.backend.util.SecurityUtils> mocked = mockStatic(
                org.example.backend.util.SecurityUtils.class)) {
            mocked.when(org.example.backend.util.SecurityUtils::getCurrentUserId).thenReturn("user-7");
            when(n8nWorkflowTriggerService.triggerAiAgent("user-7", "What is the weather?")).thenReturn(null);

            mockMvc.perform(post("/api/workflow/agent/ask")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"query\": \"What is the weather?\"}"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.response").value("Failed to get agent response"))
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}
