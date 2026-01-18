package com.example.IndiChessBackend.oauth;

import com.example.IndiChessBackend.model.User;
import com.example.IndiChessBackend.repo.UserRepo;
import com.example.IndiChessBackend.service.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepo userRepo;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        // 1️⃣ Extract stable identifiers
        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");

        if (email == null) {
            // OAuth provider misconfigured
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Email not provided by OAuth provider");
            return;
        }

        // 2️⃣ Find or create user
        User user = userRepo.getUserByEmailId(email);

        if (user == null) {
            user = new User();
            user.setEmailId(email);
            user.setUsername(name);
            userRepo.save(user);
        }

        // 3️⃣ Generate JWT using a STABLE subject
        String jwt = jwtService.generateToken(user.getEmailId());

        // 4️⃣ Store JWT in HttpOnly cookie
        Cookie jwtCookie = new Cookie("JWT", jwt);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(24 * 60 * 60); // 1 day

        // IMPORTANT:
        // setSecure(true) ONLY when using HTTPS
        jwtCookie.setSecure(false);

        response.addCookie(jwtCookie);

        // 5️⃣ Redirect to frontend (OAuth flow ends here)
        response.sendRedirect("http://localhost:3000/oauth-success");
    }
}
