package ee.tenman.auth.controller

import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class HomeController {

    @GetMapping("/home", "/")
    fun home(response: HttpServletResponse) = response.html(HOME)

}

private val HOME = template("home")
