package ee.tenman.auth.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler
import org.springframework.security.web.savedrequest.NullRequestCache

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Value("\${redirect.url}")
    private lateinit var redirectUrl: String

    @Value("\${allowed.emails}")
    private lateinit var allowedEmails: String

    @Bean
    fun securityFilterChain(http: HttpSecurity, clientRegistrations: ClientRegistrationRepository): SecurityFilterChain {
        http
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers("/login", "/logout").permitAll()
                    .requestMatchers("/oauth2/**").permitAll()
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers("/home").authenticated()
                    .anyRequest().authenticated()
            }
            .exceptionHandling { exceptions ->
                exceptions.authenticationEntryPoint(ReturnPathEntryPoint())
            }
            // sign-in returns to RETURN_PATH, so a saved request would only open a session for every signed-out visit
            .requestCache { it.requestCache(NullRequestCache()) }
            .oauth2Login { oauth2 ->
                oauth2
                    .loginPage("/login")
                    .authorizationEndpoint { endpoint ->
                        endpoint.authorizationRequestResolver(
                            ReturnPathResolver(
                                DefaultOAuth2AuthorizationRequestResolver(clientRegistrations, DEFAULT_AUTHORIZATION_REQUEST_BASE_URI)
                            )
                        )
                    }
                    .successHandler(authenticationSuccessHandler())
                    // the default keeps the error in the session, the client secret inside it included; the login page never shows it
                    .failureHandler { _, response, _ -> response.sendRedirect("/login?error") }
            }
            .csrf { it.ignoringRequestMatchers("/logout") }
            .logout { logout ->
                logout
                    .logoutUrl("/logout")
                    .logoutSuccessHandler(logoutSuccessHandler())
                    .invalidateHttpSession(true)
                    .deleteCookies("AUTHSESSION")
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

}
