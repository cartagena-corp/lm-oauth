package com.cartagenacorp.lm_oauth.security;

import com.cartagenacorp.lm_oauth.entity.User;
import com.cartagenacorp.lm_oauth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String googleId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String firstName = oAuth2User.getAttribute("given_name");
        String lastName = oAuth2User.getAttribute("family_name");

        Optional<User> usuarioExistente = userRepository.findByGoogleId(googleId);
        User user;

        if (usuarioExistente.isPresent()) {
            user = usuarioExistente.get();
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
        } else {
            user = new User();
            user.setGoogleId(googleId);
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
        }

        userRepository.save(user);

        return new CustomOAuth2User(oAuth2User, user.getId());
    }
}
