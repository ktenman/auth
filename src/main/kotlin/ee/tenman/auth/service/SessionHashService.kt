package ee.tenman.auth.service

import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpSession
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.stereotype.Service
import java.security.MessageDigest

@Service
@EnableAsync
class SessionHashService {

    private val log = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun init() {
        log.info("GOOGLE_CLIENT_ID: " + System.getenv("GOOGLE_CLIENT_ID"))
        log.info("GOOGLE_CLIENT_SECRET: " + System.getenv("GOOGLE_CLIENT_SECRET"))
        log.info("REDIRECT_URI: " + System.getenv("REDIRECT_URI"))
        log.info("ALLOWED_EMAILS: " + System.getenv("ALLOWED_EMAILS"))
    }

    fun validateHash(session: HttpSession): Boolean {
        val storedHash = session.getAttribute("SESSION_HASH") as? String ?: return false
        val currentHash = generateHash(session.id)
        return storedHash == currentHash
    }

    private fun generateHash(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
