package com.cartagenacorp.lm_oauth.service;

import com.cartagenacorp.lm_oauth.dto.UserDTO;
import com.cartagenacorp.lm_oauth.entity.User;
import com.cartagenacorp.lm_oauth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public User register(UserDTO userDTO) {
        User user = new User();
        user.setEmail(userDTO.getEmail());
        return userRepository.save(user);
    }

    public User login(UserDTO userDTO) {
        return userRepository.findByEmail(userDTO.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
    }
}
