package ee.tenman.auth.controller

import ee.tenman.auth.config.aspect.Loggable
import ee.tenman.auth.model.AuthResponse
import ee.tenman.auth.model.AuthStatus
import ee.tenman.auth.model.UserInfo
import ee.tenman.auth.service.CacheService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
class AuthController(
    private val cacheService: CacheService,
    @Value("\${allowed.emails}") private val allowedEmails: List<String>
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Loggable
    @GetMapping("/user", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun user(authentication: Authentication): AuthResponse {
        return handleAuthentication(authentication)
    }

    @Loggable
    @GetMapping("/user-by-session", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun userBySession(@RequestParam sessionId: String): AuthResponse {
        log.info("Checking session: $sessionId")
        val decodedSessionId = try {
            String(Base64.getDecoder().decode(sessionId))
        } catch (e: IllegalArgumentException) {
            log.error("Invalid base64 sessionId: $sessionId")
            return createUnauthorizedResponse("Invalid session ID")
        }

        val authentication = cacheService.getAuthentication(decodedSessionId)
            ?: return createUnauthorizedResponse("No authentication found for session")

        return handleAuthentication(authentication)
    }

    private fun handleAuthentication(authentication: Authentication): AuthResponse {
        if (authentication !is OAuth2AuthenticationToken) {
            return createUnauthorizedResponse("Invalid authentication type")
        }

        val principal = authentication.principal
        val email = principal.attributes["email"] as String? ?: return createUnauthorizedResponse("Email not found")

        if (!allowedEmails.contains(email)) {
            log.error("Unauthorized access attempt by email: $email")
            return createUnauthorizedResponse("User not authorized")
        }

        log.info("User $email authenticated successfully")
        return createAuthorizedResponse(email, principal, authentication)
    }

    private fun createUnauthorizedResponse(message: String): AuthResponse =
        AuthResponse(
            status = AuthStatus.UNAUTHORIZED,
            message = message
        )

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

    @Loggable
    @GetMapping("/validate", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun validateSession(authentication: Authentication): ResponseEntity<Map<String, String>> {
        log.info("Validating session")
        val email = (authentication.principal as? OAuth2User)?.attributes?.get("email") as? String
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Invalid authentication"))

        if (!allowedEmails.contains(email)) {
            log.error("Unauthorized session validation attempt by email: $email")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "Unauthorized: Session validation failed"))
        }

        log.info("Session validated for email: $email")
        return ResponseEntity.ok(mapOf("message" to "Session validated successfully"))
    }
}
