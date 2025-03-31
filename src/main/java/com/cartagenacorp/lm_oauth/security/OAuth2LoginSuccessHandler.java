package com.cartagenacorp.lm_oauth.security;

import com.cartagenacorp.lm_oauth.util.JwtTokenUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        String token = jwtTokenUtil.generateToken(oAuth2User.getUserId().toString(), oAuth2User.getEmail());

//        // Redirigir al frontend con el token
//        String redirectUrl = "http://tu-frontend-url/oauth2/redirect?token=" + token;
//        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
        // Configuración de la respuesta HTTP
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"token\": \"" + token + "\"}");
        response.getWriter().flush();
    }
}
