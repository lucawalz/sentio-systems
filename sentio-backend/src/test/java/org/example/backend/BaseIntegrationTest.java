package org.example.backend;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@Import(IntegrationTestConfig.class)
@Transactional
public abstract class BaseIntegrationTest {

    static {
        System.setProperty("api.version", "1.44");
    }

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withCommand("-c", "max_connections=1000")
            .withReuse(true);

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "2");

        // Disable external services for test isolation
        registry.add("mqtt.enabled", () -> "false");
        registry.add("queue.enabled", () -> "false");
        registry.add("preprocessing.service.enabled", () -> "false");
        registry.add("openmeteo.forecast.enabled", () -> "false");
        registry.add("brightsky.alerts.enabled", () -> "false");

        // Minimize scheduled task threads
        registry.add("spring.task.scheduling.pool.size", () -> "1");
    }
}
