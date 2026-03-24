package dev.syslabs.sentio;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@Import(IntegrationTestConfig.class)
class SentioApplicationTests {

	// Workaround for Docker Desktop v29+ (Docker Engine API 1.44+)
	static {
		System.setProperty("api.version", "1.44");
	}

	@ServiceConnection
	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
			.withReuse(true);

	static {
		postgres.start();
	}

	@Test
	void contextLoads() {
	}

	@Test
	void mainMethodStartsApplication() {
		try (org.mockito.MockedStatic<org.springframework.boot.SpringApplication> mocked = org.mockito.Mockito
				.mockStatic(org.springframework.boot.SpringApplication.class)) {
			SentioApplication.main(new String[] {});
			mocked.verify(
					() -> org.springframework.boot.SpringApplication.run(SentioApplication.class, new String[] {}),
					org.mockito.Mockito.times(1));
		}
	}

}
