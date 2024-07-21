package ee.tenman.auth.controller

import ee.tenman.auth.service.SessionHashService
import jakarta.servlet.http.HttpSession
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
class AuthController(
    private val sessionHashService: SessionHashService,
    @Value("\${allowed.emails}") private val allowedEmails: List<String>
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/user")
    fun user(authentication: Authentication): ResponseEntity<Map<String, Any>> =
        when (authentication) {
            is OAuth2AuthenticationToken -> {
                val email = (authentication.principal.attributes["email"] as String?)
                if (email != null && allowedEmails.contains(email)) {
                    log.info("User $email logged in successfully")
                    ResponseEntity.ok(authentication.principal.attributes)
                } else {
                    log.error("Unauthorized access attempt by email: $email")
                    ResponseEntity(mapOf("error" to "Unauthorized"), HttpStatus.UNAUTHORIZED)
                }
            }
            else -> ResponseEntity(mapOf("error" to "Unauthorized"), HttpStatus.UNAUTHORIZED)
        }

    @GetMapping("/validate")
    fun validateSession(authentication: Authentication, session: HttpSession): ResponseEntity<Map<String, String>> {
        val email = (authentication.principal as OAuth2User).attributes["email"] as String?
        return if (email != null && allowedEmails.contains(email) && sessionHashService.validateHash(session)) {
            val sessionId = session.id
            log.info("Session validated for email: $email")
            ResponseEntity.ok(mapOf("sessionId" to sessionId))
        } else {
            log.error("Unauthorized session validation attempt by email: $email")
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized: Session validation failed")
        }
    }
}
