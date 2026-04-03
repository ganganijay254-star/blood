package com.bloodcare.bloodcare.security;

import com.bloodcare.bloodcare.entity.Admin;
import com.bloodcare.bloodcare.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                Admin admin = (Admin) session.getAttribute("admin");
                if (admin != null) {
                    List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            admin.getEmail(), null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    User user = (User) session.getAttribute("user");
                    if (user != null) {
                        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                        String role = user.getRole();
                        if (role == null || role.isBlank()) {
                            role = "USER";
                        }
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                user.getEmail(), null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
