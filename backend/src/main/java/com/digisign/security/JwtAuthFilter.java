package com.digisign.security;

import com.digisign.repository.AppUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AppUserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, AppUserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                var claims = jwtService.parse(header.substring(7));
                // Ambil ulang user dari DB agar user nonaktif / perubahan role langsung berlaku
                userRepository.findByUsernameIgnoreCase(claims.getSubject())
                        .filter(u -> u.isActive())
                        .map(UserPrincipal::of)
                        .ifPresent(p -> {
                            var auth = new UsernamePasswordAuthenticationToken(p, null, p.getAuthorities());
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        });
            } catch (Exception ignored) {
                // token tidak valid / kedaluwarsa -> request diperlakukan sebagai anonim
            }
        }
        chain.doFilter(request, response);
    }
}
