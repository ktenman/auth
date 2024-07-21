package ee.tenman.auth.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler
import org.springframework.session.web.http.CookieSerializer
import org.springframework.session.web.http.DefaultCookieSerializer

@Configuration
@EnableWebSecurity
class SecurityConfig {

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
        return http.build()
    }

    @Bean
    fun authenticationSuccessHandler(): AuthenticationSuccessHandler {
        val emailsList = allowedEmails.split(",").map { it.trim() }
        return CustomAuthenticationSuccessHandler(redirectUrl, emailsList)
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
        return serializer
    }
}
