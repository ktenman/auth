package ee.tenman.auth.controller

import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController(private val redis: RedisConnectionFactory) {

    @GetMapping("/actuator/health")
    fun health(): Map<String, String> {
        redis.connection.use { it.ping() }
        return mapOf("status" to "UP")
    }
}
