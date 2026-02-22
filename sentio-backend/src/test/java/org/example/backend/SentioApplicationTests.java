package org.example.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@Import(IntegrationTestConfig.class)
class SentioApplicationTests {

	// Workaround for Docker Desktop v29+ (Docker Engine API 1.44+)
	static {
		System.setProperty("api.version", "1.44");
	}

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
			.withReuse(true);

	@Test
	void contextLoads() {
	}

}
