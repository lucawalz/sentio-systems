package org.example.backend.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link WorkflowResult} JPA entity.
 *
 * <p>
 * These tests focus on the entity's lifecycle callback methods
 * {@code @PrePersist} and {@code @PreUpdate}.
 * </p>
 *
 * <p>
 * Since lifecycle callbacks are normally invoked by the JPA provider
 * (e.g. Hibernate), we manually trigger them via reflection to keep
 * these tests as pure unit tests without requiring a database.
 * </p>
 */
class WorkflowResultTest {

    @Test
    void onCreate_setsTimestampIfNull_andAlwaysSetsLastUpdated() throws Exception {
        // Create a new WorkflowResult instance without timestamps
        WorkflowResult result = new WorkflowResult();
        result.setTimestamp(null);
        result.setLastUpdated(null);
        result.setAnalysisText("hello");
        result.setDataConfidence(0.85f);

        // Manually invoke the @PrePersist lifecycle callback
        invokeLifecycle(result, "onCreate");

        // The timestamp must be initialized automatically if it was null
        assertNotNull(result.getTimestamp(), "timestamp should be set on create when null");
        assertNotNull(result.getLastUpdated(), "lastUpdated should always be set on create");

        // lastUpdated should not be significantly earlier than timestamp
        assertFalse(result.getLastUpdated().isBefore(result.getTimestamp()),
                "lastUpdated should be >= timestamp when timestamp is generated in onCreate");
    }

    @Test
    void onCreate_setsDefaultWorkflowType_whenNull() throws Exception {
        WorkflowResult result = new WorkflowResult();
        result.setWorkflowType(null);

        invokeLifecycle(result, "onCreate");

        assertEquals(WorkflowType.SUMMARY, result.getWorkflowType(),
                "workflowType should default to SUMMARY when null");
    }

    @Test
    void onCreate_doesNotOverrideExistingTimestamp_butUpdatesLastUpdated() throws Exception {
        // Create a WorkflowResult with a pre-defined timestamp
        LocalDateTime fixedTimestamp = LocalDateTime.of(2020, 1, 1, 12, 0);
        WorkflowResult result = new WorkflowResult();
        result.setTimestamp(fixedTimestamp);

        // Simulate the @PrePersist callback
        invokeLifecycle(result, "onCreate");

        // The original timestamp must remain unchanged
        assertEquals(fixedTimestamp, result.getTimestamp(), "timestamp must not be overridden if already set");
        assertNotNull(result.getLastUpdated(), "lastUpdated should be set on create");
    }

    @Test
    void onCreate_doesNotOverrideExistingWorkflowType() throws Exception {
        WorkflowResult result = new WorkflowResult();
        result.setWorkflowType(WorkflowType.AGENT_RESPONSE);

        invokeLifecycle(result, "onCreate");

        assertEquals(WorkflowType.AGENT_RESPONSE, result.getWorkflowType(),
                "workflowType must not be overridden if already set");
    }

    @Test
    void onUpdate_refreshesLastUpdated() throws Exception {
        // Create an existing entity with an ID and an old lastUpdated value
        WorkflowResult result = new WorkflowResult();
        result.setId(42L);

        LocalDateTime old = LocalDateTime.of(2022, 5, 10, 10, 0);
        result.setLastUpdated(old);

        // Manually invoke the @PreUpdate lifecycle callback
        invokeLifecycle(result, "onUpdate");

        // lastUpdated must be updated to a more recent timestamp
        assertNotNull(result.getLastUpdated(), "lastUpdated should not be null after update");
        assertTrue(result.getLastUpdated().isAfter(old),
                "lastUpdated should be refreshed to a later time on update");
    }

    private static void invokeLifecycle(WorkflowResult target, String methodName) throws Exception {
        Method m = WorkflowResult.class.getDeclaredMethod(methodName);
        m.setAccessible(true);
        m.invoke(target);
    }
}
