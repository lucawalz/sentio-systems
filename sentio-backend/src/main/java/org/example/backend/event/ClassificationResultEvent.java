package org.example.backend.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

/**
 * Event published when an AI classification result is received from the queue.
 * Triggers downstream processing to update the detection record.
 */
@Getter
public class ClassificationResultEvent extends ApplicationEvent {

    private final String jobId;
    private final Long detectionId;
    private final boolean success;
    private final Map<String, Object> result;

    public ClassificationResultEvent(Object source, String jobId, Long detectionId,
            boolean success, Map<String, Object> result) {
        super(source);
        this.jobId = jobId;
        this.detectionId = detectionId;
        this.success = success;
        this.result = result;
    }
}
