package com.example.IndiChessBackend.oauth;

import com.example.IndiChessBackend.service.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;



    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        String subject;

        Object principal = authentication.getPrincipal();

        // Google (OIDC) -> email is usually present
        if (principal instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser oidcUser) {
            subject = oidcUser.getEmail(); // or oidcUser.getSubject()
        } else if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User oAuth2User) {
            // e.g., Github -> depends on userinfo mapping; often id or username
            Object id = oAuth2User.getAttributes().get("id");
            subject = (id != null) ? id.toString() : authentication.getName();
        } else {
            // Default: use the username (for regular login)
            subject = authentication.getName();
        }

        // Generate the JWT token
        String jwt = jwtService.generateToken(subject);

        System.out.println(jwt);

        // Send JWT as a secure HttpOnly cookie
        Cookie cookie = new Cookie("JWT", jwt);
        cookie.setHttpOnly(true);  // This prevents client-side access to the cookie (security)
        cookie.setPath("/");       // Cookie is accessible throughout the domain
        cookie.setMaxAge(60 * 60 * 24); // Set cookie expiration (1 day, for example)
        response.addCookie(cookie);

        // Redirect to /home after successful login (you can choose to return JSON instead of redirect)
        response.sendRedirect("/home");
    }

}
