package appsso.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class LoginPageController {
    @GetMapping("/signin")
    public String signInPage() {
        return "signin";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam Map<String, Object> mParams) {
        return "login";
    }
}
