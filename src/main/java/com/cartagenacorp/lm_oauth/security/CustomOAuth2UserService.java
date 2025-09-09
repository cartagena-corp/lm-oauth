package com.cartagenacorp.lm_oauth.security;

import com.cartagenacorp.lm_oauth.entity.User;
import com.cartagenacorp.lm_oauth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        logger.info("=== [Google Login] Iniciando flujo de OAuth2 ===");

        OAuth2User oAuth2User = super.loadUser(userRequest);

        logger.debug("[Google Login] Atributos recibidos de Google: {}", oAuth2User.getAttributes());

        String googleId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String firstName = oAuth2User.getAttribute("given_name");
        String lastName = oAuth2User.getAttribute("family_name");
        String picture = oAuth2User.getAttribute("picture");

        logger.info("[Google Login] Procesando usuario con email={}", email);

        Optional<User> usuarioExistente = userRepository.findByEmail(email);

        if (usuarioExistente.isEmpty()) {
            logger.info("[Google Login] Usuario NO encontrado con email={}", email);
            throw new OAuth2AuthenticationException(new OAuth2Error("unauthorized_user"),
                    "The user is not authorized. Contact the administrator.");
        }

        User user = usuarioExistente.get();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPicture(picture);
        if (user.getGoogleId() == null || user.getGoogleId().isEmpty()) {
            user.setGoogleId(googleId);
        }

        userRepository.save(user);

        logger.info("=== [Google Login] Flujo de OAuth2 finalizado correctamente ===");

        return new CustomOAuth2User(oAuth2User, user.getId());
    }
}
