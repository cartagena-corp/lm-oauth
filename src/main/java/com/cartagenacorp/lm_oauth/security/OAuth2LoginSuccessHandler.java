package com.cartagenacorp.lm_oauth.security;

import com.cartagenacorp.lm_oauth.entity.RefreshToken;
import com.cartagenacorp.lm_oauth.entity.User;
import com.cartagenacorp.lm_oauth.repository.UserRepository;
import com.cartagenacorp.lm_oauth.service.RefreshTokenService;
import com.cartagenacorp.lm_oauth.service.RoleService;
import com.cartagenacorp.lm_oauth.util.JwtTokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleService roleService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        UUID userId = oAuth2User.getUserId();
        String email = oAuth2User.getEmail();
        String picture = oAuth2User.getPicture();

        User user = userRepository.findById(userId).orElseThrow();

        String role = user.getRole();
        List<String> permissions = roleService.getPermissionsByRole(role);

        String token = jwtTokenUtil.generateToken(userId.toString(), email, picture, role, permissions);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userId);

//        // Redirigir al frontend con el token
//        String redirectUrl = "http://tu-frontend-url/oauth2/redirect?token=" + token;
//        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
        // Configuración de la respuesta HTTP
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(new ObjectMapper().writeValueAsString(
                Map.of("token", token, "refreshToken", refreshToken.getToken())
        ));
        response.getWriter().flush();
    }
}
