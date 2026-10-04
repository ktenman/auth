package ee.tenman.auth.service

import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.stereotype.Service
import java.security.MessageDigest

@Service
@EnableAsync
class SessionHashService {

    fun validateHash(sessionId: String, storedHash: String): Boolean {
        val currentHash = generateHash(sessionId)
        return storedHash == currentHash
    }

    private fun generateHash(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
