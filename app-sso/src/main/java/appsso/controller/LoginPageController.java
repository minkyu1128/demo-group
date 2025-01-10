package appsso.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class LoginPageController {
    /**
     * 로그인 페이지
     *
     * @return
     */
    @GetMapping("/signin")
    public String signInPage() {
        return "signin";
    }

    /**
     * 로그인
     *
     * @param mParams
     * @return
     */
    @GetMapping("/login")
    public String loginPage(@AuthenticationPrincipal OAuth2User oauth2User, Model model, @RequestParam Map<String, Object> mParams) {
        String email = "";
        String nickname = "";
        String id = "";

        Map<String, Object> attributes = oauth2User.getAttributes();
        log.info("OAuth2User attributes: {}", attributes);  // 디버깅을 위한 로그 추가

        // 제공자(provider)에 따라 다르게 처리
        if (attributes.containsKey("kakao_account")) {
            // 카카오 로그인
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            Map<String, Object> kakaoProfile = (Map<String, Object>) kakaoAccount.get("profile");
            
            id = String.valueOf(attributes.get("id"));
            email = (String) kakaoAccount.get("email");
            nickname = (String) kakaoProfile.get("nickname");
            
        } else if (attributes.containsKey("response")) {
            // 네이버 로그인
            Map<String, Object> response = (Map<String, Object>) attributes.get("response");
            
            id = (String) response.get("id");
            email = (String) response.get("email");
            nickname = (String) response.get("name");
            
        } else {
            // 구글 로그인
            id = (String) attributes.get("sub");
            email = (String) attributes.get("email");
            nickname = (String) attributes.get("name");
        }

        log.info("Processed user info - id: {}, email: {}, nickname: {}", id, email, nickname);

        model.addAttribute("id", id);
        model.addAttribute("email", email);
        model.addAttribute("nickname", nickname);
        
        return "login";
    }
}
