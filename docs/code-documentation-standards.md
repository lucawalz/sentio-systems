# Code Documentation Standards

## Principles
1. All public APIs must be documented
2. Complex logic requires inline comments
3. Business rules must be explained, not just described

## JavaDoc Requirements

### Classes
Every class needs:
- Purpose description
- Author (optional)
- Since version (optional)

### Methods
Public methods need:
- Description of what the method does
- @param for each parameter
- @return description
- @throws for checked exceptions

### Example
```java
/**
 * Processes animal detection results from AI services.
 * Handles both bird and general species classifications.
 *
 * @param detection the raw detection data from preprocessing
 * @return processed DetectionResult with species information
 * @throws ClassificationException if AI service is unavailable
 */
public DetectionResult processDetection(RawDetection detection) {
    // Implementation
}
```

## OpenAPI/Swagger
All REST endpoints must have:
- @Tag for grouping
- @Operation with summary and description
- @ApiResponse for each status code
- @Parameter for request parameters

## package-info.java
Each main package must have a package-info.java with:
- Package purpose
- Main responsibilities
- Notable design decisions

## MQTT Handlers
- Document expected JSON payload structure in JavaDoc

## Coverage
- At least 80% of public methods must have JavaDoc
- Complex methods (>20 lines) require detailed JavaDoc

## Review
- Documentation is reviewed in code reviews
- Outdated or missing JavaDoc must be addressed before merging
