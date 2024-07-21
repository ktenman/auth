package ee.tenman.auth.service

import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpSession
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.security.MessageDigest


@Service
class SessionHashService {

    private val log = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun init() {
        log.info("GOOGLE_CLIENT_ID: " + System.getenv("GOOGLE_CLIENT_ID"))
        log.info("GOOGLE_CLIENT_SECRET: " + System.getenv("GOOGLE_CLIENT_SECRET"))
        log.info("REDIRECT_URI: " + System.getenv("REDIRECT_URI"))
        log.info("ALLOWED_EMAILS: " + System.getenv("ALLOWED_EMAILS"))
    }

    fun generateAndStoreHash(session: HttpSession) {
        val sessionId = session.id
        val hash = generateHash(sessionId)
        session.setAttribute("SESSION_HASH", hash)
    }

    fun getStoredHash(session: HttpSession): String? {
        return session.getAttribute("SESSION_HASH") as? String
    }

    fun validateHash(session: HttpSession): Boolean {
        val storedHash = getStoredHash(session) ?: return false
        val currentHash = generateHash(session.id)
        return storedHash == currentHash
    }

    private fun generateHash(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
