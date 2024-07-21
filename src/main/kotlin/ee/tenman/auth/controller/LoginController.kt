package ee.tenman.auth.controller

import ee.tenman.auth.service.SessionHashService
import jakarta.servlet.http.HttpSession
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class LoginController(
    private val sessionHashService: SessionHashService,
    @Value("\${spring.redirect.uri}") private val redirectUri: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/")
    fun home(): String = "home"

    @GetMapping("/login")
    fun login(): String = "login"

    @GetMapping("/dashboard")
    fun dashboard(model: Model, authentication: Authentication, session: HttpSession): String {
        val userName = (authentication.principal as OAuth2User).attributes["name"] as String
        model.addAttribute("userName", userName)

        sessionHashService.generateAndStoreHash(session)

        return "dashboard"
    }

    @GetMapping("/error")
    fun handleError(model: Model): String {
        model.addAttribute("error", "An error occurred")
        model.addAttribute("status", "404")
        return "error"
    }

    @GetMapping("/login/oauth2/code/google")
    fun handleGoogleCallback(): String {
        log.info("Redirect URI: $redirectUri")
        return "redirect:$redirectUri"
    }
}
