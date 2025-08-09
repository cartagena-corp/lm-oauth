package com.cartagenacorp.lm_oauth.security;

import com.cartagenacorp.lm_oauth.entity.RefreshToken;
import com.cartagenacorp.lm_oauth.entity.User;
import com.cartagenacorp.lm_oauth.repository.UserRepository;
import com.cartagenacorp.lm_oauth.service.RefreshTokenService;
import com.cartagenacorp.lm_oauth.service.RoleExternalService;
import com.cartagenacorp.lm_oauth.util.JwtTokenUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Value("${app.jwt.refreshExpiration}")
    private long refreshExpirationMs;

    @Value("${app.oauth2.redirect-url}")
    private String oauth2RedirectUrl;

    private final JwtTokenUtil jwtTokenUtil;

    private final UserRepository userRepository;

    private final RoleExternalService roleExternalService;

    private final RefreshTokenService refreshTokenService;

    public OAuth2LoginSuccessHandler(JwtTokenUtil jwtTokenUtil,
                                     UserRepository userRepository,
                                     RoleExternalService roleExternalService,
                                     RefreshTokenService refreshTokenService) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.userRepository = userRepository;
        this.roleExternalService = roleExternalService;
        this.refreshTokenService = refreshTokenService;
    }


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        UUID userId = oAuth2User.getUserId();
        String email = oAuth2User.getEmail();
        String givenName = oAuth2User.getFirstName();
        String familyName = oAuth2User.getLastName();
        String picture = oAuth2User.getPicture();

        User user = userRepository.findById(userId).orElseThrow();

        String role = user.getRole();
        List<String> permissions = roleExternalService.getPermissionsByRole(role);
        UUID organizationId = user.getOrganizationId();

        String token = jwtTokenUtil.generateToken(userId.toString(), email, givenName, familyName, picture, role, permissions, organizationId);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userId);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken.getToken())
                .httpOnly(true)
                .secure(true) // solo si estás usando HTTPS
                .path("/")
                .maxAge(refreshExpirationMs / 1000)
                .sameSite("Strict")
                .build();

        response.setHeader("Set-Cookie", cookie.toString());

        // Redirigir al frontend con el token
        String redirectUrl = oauth2RedirectUrl + "?token=" + token;
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
