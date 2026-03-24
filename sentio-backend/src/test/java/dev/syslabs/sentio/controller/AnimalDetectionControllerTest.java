package dev.syslabs.sentio.controller;

import dev.syslabs.sentio.dto.AnimalDetectionDTO;
import dev.syslabs.sentio.dto.AnimalDetectionSummary;
import dev.syslabs.sentio.mapper.AnimalDetectionMapper;
import dev.syslabs.sentio.model.AnimalDetection;
import dev.syslabs.sentio.service.AnimalDetectionCommandService;
import dev.syslabs.sentio.service.AnimalDetectionQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.ArgumentCaptor;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AnimalDetectionController.class, excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
})
@Import(AnimalDetectionControllerTest.TestBeans.class)
@org.springframework.test.context.TestPropertySource(properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
class AnimalDetectionControllerTest {

        @Autowired
        MockMvc mockMvc;

        @Autowired
        AnimalDetectionCommandService commandService;
        @Autowired
        AnimalDetectionQueryService queryService;
        @Autowired
        AnimalDetectionMapper mapper;

        @TestConfiguration
        static class TestBeans {
                @Bean
                AnimalDetectionCommandService commandService() {
                        return mock(AnimalDetectionCommandService.class);
                }

                @Bean
                AnimalDetectionQueryService queryService() {
                        return mock(AnimalDetectionQueryService.class);
                }

                @Bean
                AnimalDetectionMapper animalDetectionMapper() {
                        return mock(AnimalDetectionMapper.class);
                }
        }

        private AnimalDetection e1, e2;
        private AnimalDetectionDTO d1, d2;

        @BeforeEach
        void resetMocks() {
                // wichtig, damit "verifyNoMoreInteractions" nicht von anderen Tests beeinflusst
                // wird
                reset(commandService, queryService, mapper);

                e1 = new AnimalDetection();
                e2 = new AnimalDetection();

                d1 = new AnimalDetectionDTO();
                d1.setId(1L);
                d1.setAnimalType("bird");
                d1.setSpecies("Sparrow");

                d2 = new AnimalDetectionDTO();
                d2.setId(2L);
                d2.setAnimalType("mammal");
                d2.setSpecies("Fox");
        }

        @Test
        void getLatestDetections_returns200_andUsesDefaultLimit() throws Exception {
                when(queryService.getLatestDetections(10)).thenReturn(List.of(e1, e2));
                when(mapper.toDTOList(List.of(e1, e2))).thenReturn(List.of(d1, d2));

                mockMvc.perform(get("/api/animals/latest"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0].id").value(1))
                                .andExpect(jsonPath("$[1].id").value(2));

                verify(queryService).getLatestDetections(10);
                verify(mapper).toDTOList(List.of(e1, e2));
                verifyNoMoreInteractions(commandService, queryService, mapper);
        }

        @Test
        void getLatestDetections_respectsLimitParam() throws Exception {
                when(queryService.getLatestDetections(3)).thenReturn(List.of(e1));
                when(mapper.toDTOList(List.of(e1))).thenReturn(List.of(d1));

                mockMvc.perform(get("/api/animals/latest").param("limit", "3"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.length()").value(1))
                                .andExpect(jsonPath("$[0].id").value(1));

                verify(queryService).getLatestDetections(3);
                verify(mapper).toDTOList(List.of(e1));
                verifyNoMoreInteractions(commandService, queryService, mapper);
        }

        @Test
        void getRecentDetections_returns200_andUsesDefaultHours() throws Exception {
                when(queryService.getRecentDetections(24)).thenReturn(List.of(e1));
                when(mapper.toDTOList(List.of(e1))).thenReturn(List.of(d1));

                mockMvc.perform(get("/api/animals/recent"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.length()").value(1))
                                .andExpect(jsonPath("$[0].species").value("Sparrow"));

                verify(queryService).getRecentDetections(24);
                verify(mapper).toDTOList(List.of(e1));
                verifyNoMoreInteractions(commandService, queryService, mapper);
        }

        @Test
        void getDetectionsByDate_parsesIsoDate_andReturns200() throws Exception {
                LocalDate date = LocalDate.of(2025, 12, 18);
                when(queryService.getDetectionsByDate(date)).thenReturn(List.of(e1, e2));
                when(mapper.toDTOList(List.of(e1, e2))).thenReturn(List.of(d1, d2));

                mockMvc.perform(get("/api/animals/by-date").param("date", "2025-12-18"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.length()").value(2));

                verify(queryService).getDetectionsByDate(date);
                verify(mapper).toDTOList(List.of(e1, e2));
                verifyNoMoreInteractions(commandService, queryService, mapper);
        }

        @Test
        void getDetectionsBySpecies_passesPageableCorrectly() throws Exception {
                when(queryService.getDetectionsBySpecies(eq("sparrow"), any(Pageable.class)))
                                .thenReturn(List.of(e1));
                when(mapper.toDTOList(List.of(e1))).thenReturn(List.of(d1));

                mockMvc.perform(get("/api/animals/by-species")
                                .param("species", "sparrow")
                                .param("page", "1")
                                .param("size", "5"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.length()").value(1));

                var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
                verify(queryService).getDetectionsBySpecies(eq("sparrow"), pageableCaptor.capture());
                assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
                assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);

                verify(mapper).toDTOList(List.of(e1));
                verifyNoMoreInteractions(commandService, queryService, mapper);
        }

        @Test
        void getDetectionsByAnimalType_passesPageableCorrectly() throws Exception {
                when(queryService.getDetectionsByAnimalType(eq("bird"), any(Pageable.class)))
                                .thenReturn(List.of(e1));
                when(mapper.toDTOList(List.of(e1))).thenReturn(List.of(d1));

                mockMvc.perform(get("/api/animals/by-type")
                                .param("animalType", "bird")
                                .param("page", "0")
                                .param("size", "20"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

                var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
                verify(queryService).getDetectionsByAnimalType(eq("bird"), pageableCaptor.capture());
                assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
                assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);

                verify(mapper).toDTOList(List.of(e1));
                verifyNoMoreInteractions(commandService, queryService, mapper);
        }

        @Test
        void getDetectionsByDevice_passesPageableCorrectly() throws Exception {
                when(queryService.getDetectionsByDevice(eq("dev-1"), any(Pageable.class)))
                                .thenReturn(List.of(e2));
                when(mapper.toDTOList(List.of(e2))).thenReturn(List.of(d2));

                mockMvc.perform(get("/api/animals/by-device")
                                .param("deviceId", "dev-1")
                                .param("page", "2")
                                .param("size", "7"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$[0].id").value(2));

                var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
                verify(queryService).getDetectionsByDevice(eq("dev-1"), pageableCaptor.capture());
                assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
                assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(7);

                verify(mapper).toDTOList(List.of(e2));
                verifyNoMoreInteractions(commandService, queryService, mapper);
        }

        @Test
        void getDetectionSummary_returns200() throws Exception {
                AnimalDetectionSummary summary = new AnimalDetectionSummary();
                summary.setTotalDetections(42L);

                when(queryService.getDetectionSummary(24)).thenReturn(summary);

                mockMvc.perform(get("/api/animals/summary"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                // falls AnimalDetectionSummary ein Feld "totalDetections" hat (wie im
                                // Controller geloggt)
                                .andExpect(jsonPath("$.totalDetections").value(42));

                verify(queryService).getDetectionSummary(24);
                verifyNoMoreInteractions(commandService, queryService, mapper);
        }

        @Test
        void getSpeciesCount_returns200() throws Exception {
                when(queryService.getSpeciesCount(24)).thenReturn(Map.of("Sparrow", 3L, "Fox", 1L));

                mockMvc.perform(get("/api/animals/species-count"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.Sparrow").value(3))
                                .andExpect(jsonPath("$.Fox").value(1));

                verify(queryService).getSpeciesCount(24);
                verifyNoMoreInteractions(commandService, queryService, mapper);
        }

        @Test
        void getAnimalTypeCount_returns200() throws Exception {
                when(queryService.getAnimalTypeCount(24)).thenReturn(Map.of("bird", 5L, "mammal", 2L));

                mockMvc.perform(get("/api/animals/type-count"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.bird").value(5))
                                .andExpect(jsonPath("$.mammal").value(2));

                verify(queryService).getAnimalTypeCount(24);
                verifyNoMoreInteractions(commandService, queryService, mapper);
        }

        @Test
        void getHourlyActivity_parsesIsoDate_andReturns200() throws Exception {
                LocalDate date = LocalDate.of(2025, 12, 18);
                when(queryService.getHourlyActivity(date)).thenReturn(Map.of(8, 2L, 9, 5L));

                mockMvc.perform(get("/api/animals/hourly-activity").param("date", "2025-12-18"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$['8']").value(2))
                                .andExpect(jsonPath("$['9']").value(5));

                verify(queryService).getHourlyActivity(date);
                verifyNoMoreInteractions(commandService, queryService, mapper);
        }

        @Test
        void getHighConfidenceDetections_usesDefaults() throws Exception {
                when(queryService.getHighConfidenceDetections(0.8f, 24)).thenReturn(List.of(e1));
                when(mapper.toDTOList(List.of(e1))).thenReturn(List.of(d1));

                mockMvc.perform(get("/api/animals/high-confidence"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$[0].id").value(1));

                verify(queryService).getHighConfidenceDetections(0.8f, 24);
                verify(mapper).toDTOList(List.of(e1));
                verifyNoMoreInteractions(commandService, queryService, mapper);
        }

        @Test
        void getHighConfidenceDetections_respectsParams() throws Exception {
                when(queryService.getHighConfidenceDetections(0.9f, 6)).thenReturn(List.of(e2));
                when(mapper.toDTOList(List.of(e2))).thenReturn(List.of(d2));

                mockMvc.perform(get("/api/animals/high-confidence")
                                .param("minConfidence", "0.9")
                                .param("hours", "6"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$[0].id").value(2));

                verify(queryService).getHighConfidenceDetections(0.9f, 6);
                verify(mapper).toDTOList(List.of(e2));
                verifyNoMoreInteractions(commandService, queryService, mapper);
        }

        @Test
        void getDetectionById_returns200_whenFound() throws Exception {
                when(queryService.getDetectionById(1L)).thenReturn(Optional.of(e1));
                when(mapper.toDTO(e1)).thenReturn(d1);

                mockMvc.perform(get("/api/animals/1"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.species").value("Sparrow"));

                verify(queryService).getDetectionById(1L);
                verify(mapper).toDTO(e1);
                verifyNoMoreInteractions(commandService, queryService, mapper);
        }

        @Test
        void getDetectionById_returns404_whenMissing() throws Exception {
                when(queryService.getDetectionById(999L)).thenReturn(Optional.empty());

                mockMvc.perform(get("/api/animals/999"))
                                .andExpect(status().isNotFound());

                verify(queryService).getDetectionById(999L);
                verifyNoMoreInteractions(commandService, queryService, mapper);
        }

        @Test
        void recordDetection_returns200_andCallsMapperAndService() throws Exception {
                // Request JSON minimal (nur Felder, die DTO ziemlich sicher hat)
                String requestJson = """
                                {"animalType":"bird","species":"Sparrow"}
                                """;

                when(mapper.toEntity(any(AnimalDetectionDTO.class))).thenReturn(e1);
                when(commandService.saveAnimalDetection(e1)).thenReturn(e1);
                when(mapper.toDTO(e1)).thenReturn(d1);

                mockMvc.perform(post("/api/animals/detect")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.animalType").value("bird"))
                                .andExpect(jsonPath("$.species").value("Sparrow"));

                verify(mapper).toEntity(any(AnimalDetectionDTO.class));
                verify(commandService).saveAnimalDetection(e1);
                verify(mapper).toDTO(e1);
                verifyNoMoreInteractions(commandService, queryService, mapper);
        }

        @Test
        void deleteDetection_returns204_whenDeleted() throws Exception {
                when(commandService.deleteDetection(1L)).thenReturn(true);

                mockMvc.perform(delete("/api/animals/1"))
                                .andExpect(status().isNoContent());

                verify(commandService).deleteDetection(1L);
                verifyNoMoreInteractions(commandService, queryService, mapper);
        }

        @Test
        void deleteDetection_returns404_whenNotFound() throws Exception {
                when(commandService.deleteDetection(999L)).thenReturn(false);

                mockMvc.perform(delete("/api/animals/999"))
                                .andExpect(status().isNotFound());

                verify(commandService).deleteDetection(999L);
                verifyNoMoreInteractions(commandService, queryService, mapper);
        }

        @Test
        void getSystemStats_returns200() throws Exception {
                when(queryService.getSystemStats()).thenReturn(Map.of("totalDetections", 10));

                mockMvc.perform(get("/api/animals/stats"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.totalDetections").value(10));

                verify(queryService).getSystemStats();
                verifyNoMoreInteractions(commandService, queryService, mapper);
        }

        @Test
        void getAllSpecies_returns200() throws Exception {
                when(queryService.getAllSpecies()).thenReturn(List.of("Sparrow", "Fox"));

                mockMvc.perform(get("/api/animals/species"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0]").value("Sparrow"))
                                .andExpect(jsonPath("$[1]").value("Fox"));

                verify(queryService).getAllSpecies();
                verifyNoMoreInteractions(commandService, queryService, mapper);
        }

        @Test
        void getAllAnimalTypes_returns200() throws Exception {
                when(queryService.getAllAnimalTypes()).thenReturn(List.of("bird", "mammal"));

                mockMvc.perform(get("/api/animals/types"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0]").value("bird"))
                                .andExpect(jsonPath("$[1]").value("mammal"));

                verify(queryService).getAllAnimalTypes();
                verifyNoMoreInteractions(commandService, queryService, mapper);
        }

        @Test
        void getMammalDetections_returns200() throws Exception {
                when(queryService.getMammalDetections(24)).thenReturn(List.of(e2));
                when(mapper.toDTOList(List.of(e2))).thenReturn(List.of(d2));

                mockMvc.perform(get("/api/animals/mammals"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$[0].animalType").value("mammal"));

                verify(queryService).getMammalDetections(24);
                verify(mapper).toDTOList(List.of(e2));
                verifyNoMoreInteractions(commandService, queryService, mapper);
        }
}