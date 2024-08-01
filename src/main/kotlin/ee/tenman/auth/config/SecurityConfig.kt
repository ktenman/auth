package ee.tenman.auth.config

import ee.tenman.auth.repository.RedisPersistentTokenRepository
import ee.tenman.auth.service.CacheService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.security.web.authentication.RememberMeServices
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository
import org.springframework.session.web.http.CookieSerializer
import org.springframework.session.web.http.DefaultCookieSerializer
import java.util.*

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val redisConnectionFactory: RedisConnectionFactory,
    @Lazy private val userDetailsService: UserDetailsService,
    private val cacheService: CacheService
) {

    @Value("\${redirect.url}")
    private lateinit var redirectUrl: String

    @Value("\${allowed.emails}")
    private lateinit var allowedEmails: String

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers("/", "/login", "/logout").permitAll()
                    .requestMatchers("/oauth2/**").permitAll()
                    .requestMatchers("/actuator/health", "/actuator/info")
                    .permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2Login { oauth2 ->
                oauth2
                    .loginPage("/login")
                    .successHandler(authenticationSuccessHandler())
            }
            .logout { logout ->
                logout
                    .logoutUrl("/logout")
                    .logoutSuccessHandler(logoutSuccessHandler())
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .permitAll()
            }
            .rememberMe { rememberMe ->
                rememberMe
                    .rememberMeServices(rememberMeServices())
            }
        return http.build()
    }

    @Bean
    fun authenticationSuccessHandler(): AuthenticationSuccessHandler {
        val emailsList = allowedEmails.split(",").map { it.trim() }
        return CustomAuthenticationSuccessHandler(redirectUrl, emailsList, cacheService)
    }

    @Bean
    fun logoutSuccessHandler(): LogoutSuccessHandler {
        val handler = SimpleUrlLogoutSuccessHandler()
        handler.setDefaultTargetUrl("/")
        handler.setAlwaysUseDefaultTargetUrl(true)
        return handler
    }

    @Bean
    fun cookieSerializer(): CookieSerializer {
        val serializer = DefaultCookieSerializer()
        serializer.setSameSite("None")
        serializer.setUseSecureCookie(true)
        serializer.setCookieName("AUTHSESSION")
        serializer.setCookieMaxAge(604800) // 7 days
        return serializer
    }

    @Bean
    fun persistentTokenRepository(): PersistentTokenRepository {
        return RedisPersistentTokenRepository(redisConnectionFactory)
    }

    @Bean
    fun rememberMeServices(): RememberMeServices {
        val key = UUID.randomUUID().toString() // Generate a random key
        return PersistentTokenBasedRememberMeServices(
            key,
            userDetailsService,
            persistentTokenRepository()
        ).apply {
            setTokenValiditySeconds(604800) // 7 days
            setUseSecureCookie(true)
            setParameter("remember-me")
        }
    }
}
