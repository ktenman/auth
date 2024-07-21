package ee.tenman.auth.controller

import ee.tenman.auth.service.SessionHashService
import jakarta.servlet.http.HttpSession
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
class AuthController(private val sessionHashService: SessionHashService) {

    @GetMapping("/user")
    fun user(authentication: Authentication): ResponseEntity<Map<String, Any>> =
        when (authentication) {
            is OAuth2AuthenticationToken -> ResponseEntity.ok(authentication.principal.attributes)
            else -> ResponseEntity(mapOf("error" to "Unauthorized"), HttpStatus.UNAUTHORIZED)
        }

    @GetMapping("/validate")
    fun validateSession(session: HttpSession): ResponseEntity<Map<String, String>> {
        return if (sessionHashService.validateHash(session)) {
            val sessionId = session.id
            val hash = sessionHashService.getStoredHash(session)!!
            ResponseEntity.ok(mapOf("sessionId" to sessionId, "hash" to hash))
        } else {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized: Session validation failed")
        }
    }
}
