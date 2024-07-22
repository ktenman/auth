package ee.tenman.auth

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser

@SpringBootTest
class AuthApplicationTests {

	@Test
	@WithMockUser(username = "user", roles = ["USER"])
	fun contextLoads() {
	}

}
