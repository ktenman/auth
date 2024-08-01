package ee.tenman.auth.controller

import ee.tenman.auth.model.AuthResponse
import ee.tenman.auth.model.AuthStatus
import ee.tenman.auth.model.UserInfo
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
    fun user(authentication: Authentication): ResponseEntity<AuthResponse> {
        if (authentication !is OAuth2AuthenticationToken) {
            return createUnauthorizedResponse("Invalid authentication type")
        }

        val principal = authentication.principal
        val email = principal.attributes["email"] as String? ?: return createUnauthorizedResponse("Email not found")

        if (!allowedEmails.contains(email)) {
            log.error("Unauthorized access attempt by email: $email")
            return createUnauthorizedResponse("User not authorized")
        }

        log.info("User $email logged in successfully")
        return ResponseEntity.ok(createAuthorizedResponse(email, principal, authentication))
    }

    private fun createUnauthorizedResponse(message: String): ResponseEntity<AuthResponse> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            AuthResponse(
                status = AuthStatus.UNAUTHORIZED,
                message = message
            )
        )
    }

    private fun createAuthorizedResponse(
        email: String,
        principal: OAuth2User,
        authentication: OAuth2AuthenticationToken
    ): AuthResponse {
        return AuthResponse(
            status = AuthStatus.AUTHORIZED,
            user = UserInfo(
                email = email,
                name = principal.attributes["name"] as? String ?: "",
                givenName = principal.attributes["given_name"] as? String ?: "",
                familyName = principal.attributes["family_name"] as? String ?: "",
                picture = principal.attributes["picture"] as? String ?: ""
            ),
            authorities = authentication.authorities.map { it.authority },
            provider = authentication.authorizedClientRegistrationId
        )
    }

    @GetMapping("/validate")
    fun validateSession(authentication: Authentication, session: HttpSession): ResponseEntity<Map<String, String>> {
        val email = (authentication.principal as? OAuth2User)?.attributes?.get("email") as? String
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Invalid authentication"))

        if (!allowedEmails.contains(email) || !sessionHashService.validateHash(session)) {
            log.error("Unauthorized session validation attempt by email: $email")
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized: Session validation failed")
        }

        log.info("Session validated for email: $email")
        return ResponseEntity.ok(mapOf("sessionId" to session.id))
    }
}
