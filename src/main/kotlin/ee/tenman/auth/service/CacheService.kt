package ee.tenman.auth.service

import ee.tenman.auth.config.RedisConfiguration
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service

@Service
class CacheService(
    private val cacheManager: CacheManager
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getAuthentication(sessionId: String): Authentication? {
        return try {
            val cache = cacheManager.getCache(RedisConfiguration.USER_SESSION_CACHE)
            cache?.get(sessionId, Authentication::class.java)
        } catch (e: Exception) {
            log.error("Failed to retrieve authentication for sessionId: $sessionId", e)
            null
        }
    }

    fun saveAuthentication(sessionId: String, authentication: Authentication) {
        try {
            val cache = cacheManager.getCache(RedisConfiguration.USER_SESSION_CACHE)
            cache?.put(sessionId, authentication)
        } catch (e: Exception) {
            log.error("Failed to save authentication for sessionId: $sessionId", e)
        }
    }
}

