package ee.tenman.auth.config

import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import java.time.Duration

@Configuration
@EnableCaching
class RedisConfiguration {
    @Bean
    fun cacheManager(connectionFactory: RedisConnectionFactory): RedisCacheManager {
        val cacheConfigurations: MutableMap<String, RedisCacheConfiguration> = HashMap()
        cacheConfigurations[USER_SESSION_CACHE] =
            RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(30))
        cacheConfigurations[USER_SESSION_ID_CACHE] =
            RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(30))
        val defaultConfig = RedisCacheConfiguration.defaultCacheConfig().entryTtl(DEFAULT_TTL)
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build()
    }

    companion object {
        const val USER_SESSION_CACHE: String = "user-session-cache-v20"
        const val USER_SESSION_ID_CACHE: String = "user-session-id-cache-v20"
        private val DEFAULT_TTL: Duration = Duration.ofMinutes(7)
    }
}
