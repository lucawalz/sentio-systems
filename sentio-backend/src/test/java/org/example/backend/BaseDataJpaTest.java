package org.example.backend;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@org.springframework.test.context.TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create")
public abstract class BaseDataJpaTest {

    // Workaround for Docker Desktop v29+ (Docker Engine API 1.44+)
    static {
        System.setProperty("api.version", "1.44");
    }

    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withCommand("-c", "max_connections=1000")
            .withReuse(true);

    static {
        postgres.start();
    }
}
