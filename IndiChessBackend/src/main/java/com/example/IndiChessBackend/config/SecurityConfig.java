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
@Order(1)
SecurityFilterChain oauthChain(HttpSecurity http) throws Exception {
    return http
            .securityMatcher("/login", "/login/**", "/oauth2/**", "/login/oauth2/**")
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .oauth2Login(oauth -> oauth
                    .successHandler(oAuth2SuccessHandler) // return JWT JSON from backend
            )
            .httpBasic(Customizer.withDefaults())
            .build();
}

    // ✅ Chain 2: Your APIs (pure JWT, stateless)
    @Bean
    @Order(2)
    SecurityFilterChain apiChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/login/**", "/oauth2/**", "/login/oauth2/**", "/auth/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .build();
    }


}
