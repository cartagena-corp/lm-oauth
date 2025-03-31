package com.cartagenacorp.lm_oauth.security;

import com.cartagenacorp.lm_oauth.entity.User;
import com.cartagenacorp.lm_oauth.repository.UserRepository;
import com.cartagenacorp.lm_oauth.util.JwtTokenUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String requestTokenHeader = request.getHeader("Authorization");

        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            String token = requestTokenHeader.substring(7);

            if (jwtTokenUtil.validateToken(token)) {
                UUID userId = jwtTokenUtil.getUserUUIDFromToken(token);

                Optional<User> user = userRepository.findById(userId);

                if (user.isPresent()) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            user.get(), null, new ArrayList<>());

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        chain.doFilter(request, response);
    }
}
