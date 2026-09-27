package ee.tenman.auth.controller

import ee.tenman.auth.service.CacheService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(AuthController::class, properties = ["allowed.emails=mari.tamm@näide.ee"])
@MockitoBean(types = [CacheService::class])
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `validate accepts an allowed session whose request accepts only event streams`() {
        mockMvc.get("/validate") {
            accept = MediaType.TEXT_EVENT_STREAM
            with(oauth2Login().attributes { it["email"] = "mari.tamm@näide.ee" })
        }.andExpect { status { isOk() } }
    }

    @Test
    fun `validate rejects a disallowed session whose request accepts only event streams`() {
        mockMvc.get("/validate") {
            accept = MediaType.TEXT_EVENT_STREAM
            with(oauth2Login().attributes { it["email"] = "jüri.kask@näide.ee" })
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `validate rejects a session without an email whose request accepts only event streams`() {
        mockMvc.get("/validate") {
            accept = MediaType.TEXT_EVENT_STREAM
            with(oauth2Login())
        }.andExpect { status { isUnauthorized() } }
    }
}
