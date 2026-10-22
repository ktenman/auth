package ee.tenman.auth.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

const val RETURN_PATH = "RETURN_PATH"

class ReturnPathEntryPoint : LoginUrlAuthenticationEntryPoint("/login") {

    override fun determineUrlToUseForThisRequest(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException
    ): String {
        val page = request.getHeader("X-Forwarded-Uri") ?: return loginFormUrl
        return "$loginFormUrl?redirect=${URLEncoder.encode(page, StandardCharsets.UTF_8)}"
    }
}

class ReturnPathResolver(private val resolver: OAuth2AuthorizationRequestResolver) : OAuth2AuthorizationRequestResolver {

    override fun resolve(request: HttpServletRequest): OAuth2AuthorizationRequest? =
        resolver.resolve(request)?.also { remember(request) }

    override fun resolve(request: HttpServletRequest, clientRegistrationId: String): OAuth2AuthorizationRequest? =
        resolver.resolve(request, clientRegistrationId)?.also { remember(request) }

    private fun remember(request: HttpServletRequest) {
        request.session.setAttribute(RETURN_PATH, request.getParameter("redirect")?.takeIf(::isLocalPath))
    }

    private fun isLocalPath(path: String): Boolean =
        path.startsWith("/") && !path.startsWith("//") && runCatching { URI(path) }.isSuccess
}
