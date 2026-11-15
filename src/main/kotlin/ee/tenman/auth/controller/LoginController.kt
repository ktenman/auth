package ee.tenman.auth.controller

import jakarta.servlet.RequestDispatcher
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.webmvc.error.ErrorController
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.util.HtmlUtils

@Controller
class LoginController : ErrorController {

    @GetMapping("/login")
    fun login(request: HttpServletRequest, response: HttpServletResponse) =
        response.html(LOGIN.replace("<!--redirect-->", request.getParameter("redirect")?.let(::redirectInput).orEmpty()))

    @RequestMapping("/error")
    fun handleError(request: HttpServletRequest, response: HttpServletResponse) {
        val status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE) as Int? ?: HttpServletResponse.SC_NOT_FOUND
        val reason = HttpStatus.resolve(status)?.reasonPhrase ?: "An error occurred"
        response.html(ERROR.replace("<!--status-->", "<p>$reason</p>\n        <p>$status</p>"))
    }

    private fun redirectInput(page: String) =
        """<input name="redirect" value="${HtmlUtils.htmlEscape(page, "UTF-8")}" type="hidden">"""
}

private val LOGIN = template("login")
private val ERROR = template("error")

fun template(name: String): String = ClassPathResource("templates/$name.html").getContentAsString(Charsets.UTF_8)

fun HttpServletResponse.html(page: String) {
    contentType = "text/html;charset=UTF-8"
    writer.write(page)
}
