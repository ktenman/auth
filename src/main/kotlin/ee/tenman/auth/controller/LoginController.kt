package ee.tenman.auth.controller

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class LoginController {

    @GetMapping("/login")
    fun login(): String = "login"

    @GetMapping("/error")
    fun handleError(model: Model): String {
        model.addAttribute("error", "An error occurred")
        model.addAttribute("status", "404")
        return "error"
    }
}
