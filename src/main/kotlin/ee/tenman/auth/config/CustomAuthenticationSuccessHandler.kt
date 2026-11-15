package ee.tenman.auth.config

import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import java.io.IOException

class CustomAuthenticationSuccessHandler(
    private val redirectUrl: String,
    private val allowedEmails: List<String>
) : AuthenticationSuccessHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @Throws(IOException::class, ServletException::class)
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val user = authentication.principal as OAuth2User
        val email = user.attributes["email"] as String?

        if (email != null && allowedEmails.contains(email)) {
            log.info("User $email logged in successfully")
            response.sendRedirect(destination(request.session))
        } else {
            log.error("Not allowed to log in: $email")
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized: Email not allowed")
        }
    }

    private fun destination(session: HttpSession): String {
        val path = session.getAttribute(RETURN_PATH) as String? ?: return redirectUrl
        return redirectUrl.removeSuffix("/") + path
    }
}
