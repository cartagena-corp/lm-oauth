package com.cartagenacorp.lm_oauth.security;

import com.cartagenacorp.lm_oauth.entity.User;
import com.cartagenacorp.lm_oauth.repository.UserRepository;
import com.cartagenacorp.lm_oauth.service.RoleService;
import com.cartagenacorp.lm_oauth.util.JwtTokenUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleService roleService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        UUID userId = oAuth2User.getUserId();
        String email = oAuth2User.getEmail();

        User user = userRepository.findById(userId).orElseThrow();

        String role = user.getRole();
        List<String> permissions = roleService.getPermissionsByRole(role);

        String token = jwtTokenUtil.generateToken(userId.toString(), email, role, permissions);
        //String token = jwtTokenUtil.generateToken(oAuth2User.getUserId().toString(), oAuth2User.getEmail());

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
