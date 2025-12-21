package org.example.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.backend.model.AISummary;
import org.example.backend.service.AISummaryService;
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
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AISummaryController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        }
)
@Import(AISummaryControllerTest.TestBeans.class)
class AISummaryControllerTest {

    @Autowired MockMvc mockMvc;

    @Autowired AISummaryService aiSummaryService; // Mockito-mock aus TestBeans

    @AfterEach
    void clearMockInvocations() {
        clearInvocations(aiSummaryService);
    }


    @TestConfiguration
    static class TestBeans {
        @Bean
        AISummaryService aiSummaryService() {
            return mock(AISummaryService.class);
        }
    }

    private static boolean jsonContainsTextAnywhere(JsonNode node, String expected) {
        if (node == null) return false;
        if (node.isTextual() && expected.equals(node.asText())) return true;

        if (node.isObject()) {
            Iterator<JsonNode> it = node.elements();
            while (it.hasNext()) if (jsonContainsTextAnywhere(it.next(), expected)) return true;
        } else if (node.isArray()) {
            for (JsonNode child : node) if (jsonContainsTextAnywhere(child, expected)) return true;
        }
        return false;
    }

    @Test
    void getCurrentSummary_returns200_whenPresent() throws Exception {
        AISummary summary = new AISummary();
        summary.setId(1L);
        summary.setAnalysisText("Hello");
        summary.setTimestamp(LocalDateTime.of(2025, 12, 18, 10, 0));

        when(aiSummaryService.getCurrentSummary()).thenReturn(Optional.of(summary));

        mockMvc.perform(get("/api/ai-summary/current"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.analysisText").value("Hello"))
                .andExpect(jsonPath("$.timestamp").value("2025-12-18T10:00:00"));

        verify(aiSummaryService, times(1)).getCurrentSummary();
        verifyNoMoreInteractions(aiSummaryService);
    }
    @Test
    void getCurrentSummary_returns204_whenEmpty() throws Exception {
        when(aiSummaryService.getCurrentSummary()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/ai-summary/current"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(aiSummaryService).getCurrentSummary();
        verifyNoMoreInteractions(aiSummaryService);
    }

    @Test
    void getRecentSummaries_returns200_andArray() throws Exception {
        // GIVEN
        AISummary a = new AISummary();
        a.setId(1L);
        a.setAnalysisText("A");
        a.setTimestamp(LocalDateTime.of(2025, 12, 18, 9, 0));

        AISummary b = new AISummary();
        b.setId(2L);
        b.setAnalysisText("B");
        b.setTimestamp(LocalDateTime.of(2025, 12, 18, 8, 0));

        when(aiSummaryService.getRecentSummaries()).thenReturn(List.of(a, b));

        // WHEN + THEN
        mockMvc.perform(get("/api/ai-summary/recent"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].analysisText").value("A"))
                .andExpect(jsonPath("$[1].analysisText").value("B"));

        verify(aiSummaryService, times(1)).getRecentSummaries();
        verifyNoMoreInteractions(aiSummaryService);
    }

    @Test
    void createAISummary_setsTimestamp_whenNull() throws Exception {
        when(aiSummaryService.saveAISummary(any(AISummary.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/ai-summary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        ArgumentCaptor<AISummary> captor = ArgumentCaptor.forClass(AISummary.class);
        verify(aiSummaryService).saveAISummary(captor.capture());

        assertThat(captor.getValue().getTimestamp()).isNotNull();

        verifyNoMoreInteractions(aiSummaryService);
    }

    @Test
    void cleanupOldSummaries_callsService_andReturns200() throws Exception {
        doNothing().when(aiSummaryService).cleanupOldSummaries();

        mockMvc.perform(post("/api/ai-summary/cleanup"))
                .andExpect(status().isOk())
                .andExpect(content().string("Cleanup completed successfully"));

        verify(aiSummaryService).cleanupOldSummaries();
        verifyNoMoreInteractions(aiSummaryService);
    }
}