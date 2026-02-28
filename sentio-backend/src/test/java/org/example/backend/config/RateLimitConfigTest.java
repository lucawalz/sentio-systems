package org.example.backend.config;

import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.codec.RedisCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitConfig Unit Tests")
class RateLimitConfigTest {

    private RateLimitConfig config;

    @BeforeEach
    void setUp() {
        config = new RateLimitConfig();
        ReflectionTestUtils.setField(config, "redisHost", "localhost");
        ReflectionTestUtils.setField(config, "redisPort", 6379);
        ReflectionTestUtils.setField(config, "redisPassword", "");
        ReflectionTestUtils.setField(config, "redisDatabase", 0);
    }

    @SuppressWarnings("unchecked")
    private StatefulRedisConnection<String, byte[]> mockConnection(RedisClient redisClient) {
        StatefulRedisConnection<String, byte[]> connection = mock(StatefulRedisConnection.class);
        RedisAsyncCommands<String, byte[]> asyncCommands = mock(RedisAsyncCommands.class);
        when(connection.async()).thenReturn(asyncCommands);
        when(redisClient.connect(any(RedisCodec.class))).thenReturn(connection);
        return connection;
    }

    @Test
    @DisplayName("Creates LettuceBasedProxyManager without password")
    void lettuceBasedProxyManager_withoutPassword_createsManager() {
        try (MockedStatic<RedisClient> redisClientStatic = mockStatic(RedisClient.class)) {
            RedisClient mockClient = mock(RedisClient.class);
            redisClientStatic.when(() -> RedisClient.create(any(RedisURI.class))).thenReturn(mockClient);
            mockConnection(mockClient);

            LettuceBasedProxyManager<String> manager = config.lettuceBasedProxyManager();

            assertThat(manager).isNotNull();
            redisClientStatic.verify(() -> RedisClient.create(any(RedisURI.class)));
        }
    }

    @Test
    @DisplayName("Sets password on RedisURI when password is configured")
    void lettuceBasedProxyManager_withPassword_setsPasswordOnUri() {
        ReflectionTestUtils.setField(config, "redisPassword", "secret123");

        try (MockedStatic<RedisClient> redisClientStatic = mockStatic(RedisClient.class)) {
            RedisClient mockClient = mock(RedisClient.class);
            ArgumentCaptor<RedisURI> uriCaptor = ArgumentCaptor.forClass(RedisURI.class);
            redisClientStatic.when(() -> RedisClient.create(uriCaptor.capture())).thenReturn(mockClient);
            mockConnection(mockClient);

            config.lettuceBasedProxyManager();

            RedisURI capturedUri = uriCaptor.getValue();
            assertThat(capturedUri.getPassword()).isEqualTo("secret123".toCharArray());
        }
    }

    @Test
    @DisplayName("Uses configured host, port, and database for RedisURI")
    void lettuceBasedProxyManager_usesConfiguredRedisSettings() {
        ReflectionTestUtils.setField(config, "redisHost", "redis.example.com");
        ReflectionTestUtils.setField(config, "redisPort", 6380);
        ReflectionTestUtils.setField(config, "redisDatabase", 2);

        try (MockedStatic<RedisClient> redisClientStatic = mockStatic(RedisClient.class)) {
            RedisClient mockClient = mock(RedisClient.class);
            ArgumentCaptor<RedisURI> uriCaptor = ArgumentCaptor.forClass(RedisURI.class);
            redisClientStatic.when(() -> RedisClient.create(uriCaptor.capture())).thenReturn(mockClient);
            mockConnection(mockClient);

            config.lettuceBasedProxyManager();

            RedisURI capturedUri = uriCaptor.getValue();
            assertThat(capturedUri.getHost()).isEqualTo("redis.example.com");
            assertThat(capturedUri.getPort()).isEqualTo(6380);
            assertThat(capturedUri.getDatabase()).isEqualTo(2);
        }
    }
}
