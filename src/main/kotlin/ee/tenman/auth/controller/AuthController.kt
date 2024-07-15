package ee.tenman.auth.controller

import jakarta.servlet.http.HttpSession
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthController {

    @GetMapping("/user")
    fun user(authentication: OAuth2AuthenticationToken): Map<String, Any> {
        return authentication.principal.attributes
    }

    @GetMapping("/validate")
    fun validateSession(session: HttpSession): String {
        return "Valid session: ${session.id}"
    }
}
