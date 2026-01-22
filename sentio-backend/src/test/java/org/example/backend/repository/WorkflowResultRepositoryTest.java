package org.example.backend.repository;

import org.example.backend.model.WorkflowResult;
import org.example.backend.model.WorkflowType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowResultRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private WorkflowResultRepository repository;

    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        now = LocalDateTime.now();
    }

    @Test
    void testFindTopByOrderByTimestampDesc() {
        repository.save(createResult(WorkflowType.WEATHER_SUMMARY, now.minusHours(2)));
        repository.save(createResult(WorkflowType.SIGHTINGS_SUMMARY, now.minusHours(1))); // Newest

        Optional<WorkflowResult> top = repository.findTopByOrderByTimestampDesc();

        assertThat(top).isPresent();
        assertThat(top.get().getTimestamp()).isEqualTo(now.minusHours(1));
    }

    @Test
    void testCountTodaysResultsByType() {
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        repository.save(createResult(WorkflowType.WEATHER_SUMMARY, startOfDay.plusHours(1)));
        repository.save(createResult(WorkflowType.WEATHER_SUMMARY, startOfDay.plusHours(2)));
        repository.save(createResult(WorkflowType.SIGHTINGS_SUMMARY, startOfDay.plusHours(3)));

        long count = repository.countTodaysResultsByType(WorkflowType.WEATHER_SUMMARY, startOfDay, endOfDay);

        assertThat(count).isEqualTo(2);
    }

    @Test
    void testFindByUserIdAndWorkflowTypeAndTimestampAfter() {
        String userId = "user1";
        repository.save(createResultUser(userId, WorkflowType.AGENT_RESPONSE, now.minusMinutes(10)));
        repository.save(createResultUser(userId, WorkflowType.AGENT_RESPONSE, now.minusMinutes(30)));
        repository.save(createResultUser("otherUser", WorkflowType.AGENT_RESPONSE, now.minusMinutes(5)));

        List<WorkflowResult> results = repository.findByUserIdAndWorkflowTypeAndTimestampAfterOrderByTimestampDesc(
                userId, WorkflowType.AGENT_RESPONSE, now.minusHours(1));

        assertThat(results).hasSize(2);
        assertThat(results).extracting(WorkflowResult::getUserId).containsOnly(userId);
    }

    private WorkflowResult createResult(WorkflowType type, LocalDateTime timestamp) {
        return createResultUser("system", type, timestamp);
    }

    private WorkflowResult createResultUser(String userId, WorkflowType type, LocalDateTime timestamp) {
        WorkflowResult res = new WorkflowResult();
        res.setWorkflowType(type);
        res.setTimestamp(timestamp);
        res.setUserId(userId);
        res.setAnalysisText("Dummy Content");
        return res;
    }
}
