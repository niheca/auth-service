package org.example.authservice2.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.authservice2.commons.entities.UserModel;
import org.example.authservice2.commons.repositories.UserRepository;
import org.example.authservice2.services.impl.JwtServiceImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtServiceImpl jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtServiceImpl jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Optional.ofNullable(request.getHeader(HttpHeaders.AUTHORIZATION))
                .filter(header -> !header.isBlank())
                .map(header -> header.substring(7))
                .map(jwtService::extractUserId)
                .flatMap(userId -> userRepository.findById(Long.valueOf(userId)))
                .ifPresent(userDetails -> {
                    request.setAttribute("X-User-Id" , userDetails.getId());
                    processAuthentication(request,userDetails);
                });

        filterChain.doFilter(request, response);

    }

    private void processAuthentication(HttpServletRequest request, UserModel userDetails) {
        String jwtToken = request.getHeader(HttpHeaders.AUTHORIZATION).substring(7);
        Optional.of(jwtToken)
                .filter(token -> !jwtService.isExpired(token))
                .ifPresent(token -> {
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                });
    }
}