package org.example.backend.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link AISummary} JPA entity.
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

class AISummaryTest {
    @Test
    void onCreate_setsTimestampIfNull_andAlwaysSetsLastUpdated() throws Exception {

        // Create a new AISummary instance without timestamps to simulate a newly created entity before persistence.
        AISummary summary = new AISummary();
        summary.setTimestamp(null);
        summary.setLastUpdated(null);
        summary.setAnalysisText("hello");
        summary.setDataConfidence(0.85f);

        //  Manually invoke the @PrePersist lifecycle callback.This simulates what JPA would do before inserting the entity.
        invokeLifecycle(summary, "onCreate");

        // The timestamp must be initialized automatically if it was null.
        assertNotNull(summary.getTimestamp(), "timestamp should be set on create when null"); //on create just if timestamp = null
        assertNotNull(summary.getLastUpdated(), "lastUpdated should always be set on create"); //always

        //lastUpdated should not be significantly earlier than timestamp.
        assertFalse(summary.getLastUpdated().isBefore(summary.getTimestamp()),
                "lastUpdated should be >= timestamp when timestamp is generated in onCreate");
    }

    @Test
    void onCreate_doesNotOverrideExistingTimestamp_butUpdatesLastUpdated() throws Exception {
        // Create an AISummary with a pre-defined timestamp.
        LocalDateTime fixedTimestamp = LocalDateTime.of(2020, 1, 1, 12, 0);
        AISummary summary = new AISummary();
        summary.setTimestamp(fixedTimestamp);

        // Simulate the @PrePersist callback.
        invokeLifecycle(summary, "onCreate");

        // The original timestamp must remain unchanged.
        assertEquals(fixedTimestamp, summary.getTimestamp(), "timestamp must not be overridden if already set");
        assertNotNull(summary.getLastUpdated(), "lastUpdated should be set on create"); // lastUpdated must still be initialized.
    }

    @Test
    void onUpdate_refreshesLastUpdated() throws Exception {
        //  Create an existing entity with an ID and an old lastUpdated value.
        AISummary summary = new AISummary();
        summary.setId(42L);

        LocalDateTime old = LocalDateTime.of(2022, 5, 10, 10, 0);
        summary.setLastUpdated(old);

        //  Manually invoke the @PreUpdate lifecycle callback.
        invokeLifecycle(summary, "onUpdate");

        // lastUpdated must be updated to a more recent timestamp.
        assertNotNull(summary.getLastUpdated(), "lastUpdated should not be null after update");
        assertTrue(summary.getLastUpdated().isAfter(old),
                "lastUpdated should be refreshed to a later time on update");
    }

    private static void invokeLifecycle(AISummary target, String methodName) throws Exception {
        Method m = AISummary.class.getDeclaredMethod(methodName);
        m.setAccessible(true);
        m.invoke(target);
    }
}
