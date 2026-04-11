package com.bloodcare.bloodcare;

import com.bloodcare.bloodcare.entity.User;
import com.bloodcare.bloodcare.repository.UserRepository;
import com.bloodcare.bloodcare.security.SessionAuthenticationFilter;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {
    private final SessionAuthenticationFilter sessionAuthenticationFilter;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder oauthPasswordEncoder = new BCryptPasswordEncoder();
    private final List<String> allowedOrigins;

    public SecurityConfig(SessionAuthenticationFilter sessionAuthenticationFilter,
                          UserRepository userRepository,
                          @Value("${app.security.allowed-origins:http://localhost:8082,http://127.0.0.1:8082}") String allowedOrigins) {
        this.sessionAuthenticationFilter = sessionAuthenticationFilter;
        this.userRepository = userRepository;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .collect(Collectors.toList());
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .addFilterBefore(sessionAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/login",
                    "/signup",
                    "/admin",
                    "/register",
                    "/donor-dashboard",
                    "/donor-profile",
                    "/visit",
                    "/visit-history",
                    "/request-visit",
                    "/donor-form",
                    "/receiver-form",
                    "/receiver-track",
                    "/medicine",
                    "/leaderboard",
                    "/about",
                    "/certificate",
                    "/certificate/**",
                    "/verify-certificate",
                    "/verify-certificate/**",
                    "/api/auth/**",
                    "/api/admin/login",
                    "/api/admin/check-session",
                    "/api/admin/logout",
                    "/api/admin/public-overview",
                    "/api/certificate/verify/**",
                    "/api/certificate/public-base-url",
                    "/api/admin/public-config",
                    "/api/hospitals/nearby",
                    "/oauth2/**",
                    "/login/oauth2/**",
                    "/favicon.ico",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/uploads/**"
                ).permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/hospital-stock/**").hasAnyRole("ADMIN", "HOSPITAL")
                .requestMatchers("/api/donor/**").hasAnyRole("ADMIN", "DONOR", "USER")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth -> oauth
                .loginPage("/login")
                .successHandler((request, response, authentication) -> {
                    User sessionUser = ensureGoogleUser(authentication);
                    if (sessionUser.isBlocked() && sessionUser.isBlockedByAdmin()) {
                        HttpSession session = request.getSession(false);
                        if (session != null) {
                            session.removeAttribute("user");
                        }
                        response.sendRedirect("/login?google=blocked");
                        return;
                    }

                    sessionUser.setPassword(null);
                    request.getSession(true).setAttribute("user", sessionUser);
                    response.sendRedirect("/");
                })
                .failureHandler((request, response, exception) ->
                    response.sendRedirect("/login?google=failed"))
            )
            .formLogin(form -> form.disable())
            .httpBasic(httpBasic -> httpBasic.disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private User ensureGoogleUser(Authentication authentication) throws IOException {
        if (!(authentication.getPrincipal() instanceof OAuth2User oauthUser)) {
            throw new IOException("Google login user details were not available");
        }

        String email = valueAsString(oauthUser.getAttribute("email"));
        if (email == null || email.isBlank()) {
            throw new IOException("Google account email was not provided");
        }

        String resolvedName = valueAsString(oauthUser.getAttribute("name"));
        if (resolvedName == null || resolvedName.isBlank()) {
            resolvedName = email;
        }

        User user = userRepository.findByEmail(email);
        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setName(resolvedName);
            user.setRole("USER");
            user.setPassword(oauthPasswordEncoder.encode(UUID.randomUUID().toString()));
            return userRepository.save(user);
        }

        boolean changed = false;
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(resolvedName);
            changed = true;
        }
        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("USER");
            changed = true;
        }

        if (changed) {
            user = userRepository.save(user);
        }
        return user;
    }

    private String valueAsString(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }
}
