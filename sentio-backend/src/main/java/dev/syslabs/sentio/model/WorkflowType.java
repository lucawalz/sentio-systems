package dev.syslabs.sentio.model;

/**
 * Defines the type of workflow result stored in the system.
 * Used to differentiate between different n8n workflow outputs.
 */
public enum WorkflowType {
    /**
     * Legacy AI-generated summary (global, not user-specific).
     * 
     * @deprecated Use WEATHER_SUMMARY or SIGHTINGS_SUMMARY instead
     */
    @Deprecated
    SUMMARY,

    /**
     * Weather-focused summary for a specific user.
     * Includes sensor data analysis, forecasts, and weather alerts.
     */
    WEATHER_SUMMARY,

    /**
     * Wildlife sightings summary for a specific user.
     * Includes detection patterns, species diversity, and activity trends.
     */
    SIGHTINGS_SUMMARY,

    /**
     * Response from the AI Agent to a user query.
     * Generated on-demand when users interact with the AI assistant.
     */
    AGENT_RESPONSE
}
