package ee.tenman.auth

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.session.web.http.CookieSerializer
import kotlin.test.assertContains

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthApplicationTests {

	@Autowired
	private lateinit var cookieSerializer: CookieSerializer

	@Test
	@WithMockUser(username = "user", roles = ["USER"])
	fun contextLoads() {
	}

	@Test
	fun `the session cookie stays off requests from other sites`() {
		val response = MockHttpServletResponse()
		cookieSerializer.writeCookieValue(CookieSerializer.CookieValue(MockHttpServletRequest(), response, "session"))
		assertContains(response.getHeader(HttpHeaders.SET_COOKIE).orEmpty(), "SameSite=Lax")
	}

}
