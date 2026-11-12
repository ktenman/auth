package ee.tenman.auth.config

import ee.tenman.auth.controller.LoginController
import ee.tenman.auth.service.CacheService
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@WebMvcTest(
    LoginController::class,
    properties = [
        "redirect.url=https://fov.ee",
        "allowed.emails=mari.tamm@näide.ee",
        "spring.security.oauth2.client.registration.google.client-id=fov",
        "spring.security.oauth2.client.registration.google.client-secret=saladus"
    ]
)
@Import(SecurityConfig::class)
@MockitoBean(types = [CacheService::class, RedisConnectionFactory::class, PersistentTokenRepository::class])
class SecurityConfigTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var successHandler: AuthenticationSuccessHandler

    @Autowired
    private lateinit var cacheService: CacheService

    @Test
    fun `validate sends a visitor without a session to sign in with the page they asked for`() {
        mockMvc.get("/validate") {
            accept = MediaType.APPLICATION_JSON
            header("X-Forwarded-Uri", "/radio/2026-10-22-1200-sihik")
        }.andExpect { redirectedUrl("/login?redirect=%2Fradio%2F2026-10-22-1200-sihik") }
    }

    @Test
    fun `login page sends the page to return to with the Google sign-in`() {
        mockMvc.get("/login") {
            param("redirect", "/radio/catch-up?day=2026-10-21")
        }.andExpect {
            content { string(containsString("""<input name="redirect" value="/radio/catch-up?day=2026-10-21" type="hidden">""")) }
        }
    }

    @Test
    fun `signing in returns to the page it started from`() {
        assertEquals("https://fov.ee/radio/catch-up?day=2026-10-21", signIn("/radio/catch-up?day=2026-10-21"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["//evil.example", "https://evil.example"])
    fun `signing in from a page on another site returns to the home page`(page: String) {
        assertEquals("https://fov.ee", signIn(page))
    }

    @Test
    fun `logout without a token signs the session out and expires its cookies`() {
        val session = signedIn()
        mockMvc.post("/logout") { this.session = session }.andExpect {
            redirectedUrl("/")
            cookie {
                maxAge("AUTHSESSION", 0)
                maxAge("remember-me", 0)
            }
        }
        assertTrue(session.isInvalid)
    }

    @Test
    fun `logout by GET leaves the session signed in`() {
        val session = signedIn()
        mockMvc.get("/logout") { this.session = session }.andExpect { status { isNotFound() } }
        assertFalse(session.isInvalid)
    }

    @Test
    fun `a POST elsewhere without a token is still forbidden`() {
        mockMvc.post("/validate") { session = signedIn() }.andExpect { status { isForbidden() } }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = [MediaType.APPLICATION_JSON_VALUE])
    fun `validate sends a signed-out session to sign in`(accept: String?) {
        val session = signedIn()
        mockMvc.post("/logout") { this.session = session }
        mockMvc.get("/validate") {
            this.session = session
            accept?.let { header(HttpHeaders.ACCEPT, it) }
        }.andExpect {
            status { isFound() }
            redirectedUrl("/login")
        }
    }

    @Test
    fun `logout forgets the authentication saved for the session`() {
        val session = signedIn()
        mockMvc.post("/logout") { this.session = session }
        verify(cacheService).evictAuthentication(session.id)
    }

    private fun signedIn() = MockHttpSession().apply {
        setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextImpl(google()))
    }

    private fun signIn(page: String): String? {
        val session = mockMvc.get("/oauth2/authorization/google") { param("redirect", page) }.andReturn().request.session
        val response = MockHttpServletResponse()
        successHandler.onAuthenticationSuccess(MockHttpServletRequest().apply { setSession(session) }, response, google())
        return response.redirectedUrl
    }

    private fun google(): OAuth2AuthenticationToken {
        val user = DefaultOAuth2User(AuthorityUtils.createAuthorityList("OAUTH2_USER"), mapOf("email" to "mari.tamm@näide.ee"), "email")
        return OAuth2AuthenticationToken(user, user.authorities, "google")
    }
}
