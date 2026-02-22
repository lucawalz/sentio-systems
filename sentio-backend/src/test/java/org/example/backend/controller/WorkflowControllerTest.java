package org.example.backend.controller;

import org.example.backend.model.WorkflowResult;
import org.example.backend.model.WorkflowType;
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
class WorkflowControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    WorkflowService workflowService;

    @AfterEach
    void clearMockInvocations() {
        clearInvocations(workflowService);
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        WorkflowService workflowService() {
            return mock(WorkflowService.class);
        }

        @Bean
        org.example.backend.service.N8nWorkflowTriggerService n8nWorkflowTriggerService() {
            return mock(org.example.backend.service.N8nWorkflowTriggerService.class);
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
        summary.setWorkflowType(WorkflowType.SUMMARY);
        summary.setTimestamp(LocalDateTime.of(2025, 12, 18, 10, 0));

        when(workflowService.getCurrentSummary()).thenReturn(Optional.of(summary));

        mockMvc.perform(get("/api/workflow/summaries/current"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.workflowType").value("SUMMARY"));

        verify(workflowService).getCurrentSummary();
        verifyNoMoreInteractions(workflowService);
    }

    @Test
    void getRecentResults_returns200_andArray() throws Exception {
        WorkflowResult a = new WorkflowResult();
        a.setId(1L);
        a.setAnalysisText("A");
        a.setWorkflowType(WorkflowType.SUMMARY);
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
        summary.setWorkflowType(WorkflowType.SUMMARY);

        when(workflowService.getRecentSummaries()).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/workflow/summaries/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].workflowType").value("SUMMARY"));

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
}
