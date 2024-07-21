package ee.tenman.auth.repository

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository
import java.util.*
import java.util.concurrent.TimeUnit

class RedisPersistentTokenRepository(connectionFactory: RedisConnectionFactory) : PersistentTokenRepository {

    private val objectMapper = ObjectMapper()
    private val redisTemplate: RedisTemplate<String, String> = RedisTemplate<String, String>().apply {
        setConnectionFactory(connectionFactory)
        keySerializer = StringRedisSerializer()
        valueSerializer = StringRedisSerializer()
        afterPropertiesSet()
    }

    private val tokenPrefix = "remember-me:token:"
    private val usernamePrefix = "remember-me:username:"

    override fun createNewToken(token: PersistentRememberMeToken) {
        val tokenJson = objectMapper.writeValueAsString(token)
        redisTemplate.opsForValue()[tokenPrefix + token.series] = tokenJson
        redisTemplate.expire(tokenPrefix + token.series, 7, TimeUnit.DAYS)
        redisTemplate.opsForValue()[usernamePrefix + token.username] = token.series
        redisTemplate.expire(usernamePrefix + token.username, 7, TimeUnit.DAYS)
    }

    override fun updateToken(series: String, tokenValue: String, lastUsed: Date) {
        val existingToken = getTokenForSeries(series)
        existingToken?.let {
            val updatedToken = PersistentRememberMeToken(it.username, series, tokenValue, lastUsed)
            val tokenJson = objectMapper.writeValueAsString(updatedToken)
            redisTemplate.opsForValue()[tokenPrefix + series] = tokenJson
            redisTemplate.expire(tokenPrefix + series, 7, TimeUnit.DAYS)
        }
    }

    override fun getTokenForSeries(seriesId: String): PersistentRememberMeToken? {
        val tokenJson = redisTemplate.opsForValue()[tokenPrefix + seriesId]
        return tokenJson?.let { objectMapper.readValue(it, PersistentRememberMeToken::class.java) }
    }

    override fun removeUserTokens(username: String) {
        val series = redisTemplate.opsForValue()[usernamePrefix + username]
        series?.let {
            redisTemplate.delete(tokenPrefix + it)
            redisTemplate.delete(usernamePrefix + username)
        }
    }
}
