package com.wordonline.admin.controller;

import com.wordonline.admin.client.AccountServerClient;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {
    private final AccountServerClient accountServerClient;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, 
                       @RequestParam String password,
                       HttpServletResponse response,
                       Model model) {
        try {
            String token = accountServerClient.login(username, password);
            
            response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie(token, 60 * 60 * 24).toString());

            return "redirect:/";
        } catch (Exception e) {
            log.error("Login failed", e);
            model.addAttribute("error", e.getMessage());
            return "login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie("", 0).toString());

        return "redirect:/login";
    }

    // Secure + SameSite=Strict is what keeps the cookie off cross-site requests and off plaintext http.
    private static ResponseCookie jwtCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from("jwt", value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }
}
