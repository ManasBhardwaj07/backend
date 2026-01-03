package com.example.IndiChessBackend.config;

import com.example.IndiChessBackend.filters.JwtFilter;
import com.example.IndiChessBackend.oauth.OAuth2SuccessHandler;
import com.example.IndiChessBackend.service.MyUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final MyUserDetailsService userDetailService;
    private final JwtFilter jwtFilter;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Bean
    PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(){
        // user details service
        DaoAuthenticationProvider auth = new DaoAuthenticationProvider(userDetailService);
        auth.setPasswordEncoder(passwordEncoder());
        return auth;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

//    @Bean
//    public SecurityFilterChain newSpringSecurityFilterChain(HttpSecurity http) throws Exception{
//        return http
//        .authorizeHttpRequests(auth -> auth
//                .requestMatchers("/login",  "/auth/**").permitAll()
//                .anyRequest().authenticated()
//        )
//                .addFilterBefore(jwtFilter,
//                        UsernamePasswordAuthenticationFilter.class)
//        .csrf(csrf -> csrf.disable())
//        .sessionManagement(session ->
//                session.sessionCreationPolicy
//                        (SessionCreationPolicy.STATELESS))
//        .httpBasic(Customizer.withDefaults())
//        .build();
//
//
////        return http
////                .csrf(csrf -> csrf.disable())
////
////                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
////
//////                .securityContext(sc -> sc.requireExplicitSave(false)) // optional, safe
//////                .requestCache(rc -> rc.disable())                     // avoids saving requests in session
////
////                .authorizeHttpRequests(auth -> auth
////                        .requestMatchers("/login", "/hello", "/auth/**").permitAll()
////                        .anyRequest().authenticated()
////                )
////
//////                .httpBasic(Customizer.withDefaults())
////                .formLogin(Customizer.withDefaults())
////                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
////                .build();
//    }

@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    // Allow only the frontend port (e.g., localhost:3000)
    configuration.addAllowedOrigin("http://localhost:3000");  // Specify frontend port here
    configuration.addAllowedMethod("*");  // Allow all HTTP methods (GET, POST, PUT, DELETE, etc.)
    configuration.addAllowedHeader("*");  // Allow all headers
    configuration.setAllowCredentials(true); // Allow cookies (if needed)

    // Register this configuration for all endpoints
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(c -> c.configurationSource(corsConfigurationSource())) // Apply CORS configuration
                .csrf(csrf -> csrf.disable())  // Disable CSRF for now (may re-enable if necessary)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/signup", "/oauth2/**", "/login/oauth2/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")               // Custom login page
                        .defaultSuccessUrl("http://localhost:3000/home", true)  // Redirect to /home after login success
                        .permitAll()
                )
                .oauth2Login(oauth -> oauth
                        .loginPage("/login") // Use custom page for OAuth login
                        .successHandler(oAuth2SuccessHandler) // Handle success with custom handler
                        .defaultSuccessUrl("http://localhost:3000/home", true)
                )
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)) // Allow session if needed
                .httpBasic(Customizer.withDefaults())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class) // JWT filter for stateless API
                .build();
    }

}
