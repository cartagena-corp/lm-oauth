package com.cartagenacorp.lm_oauth.service;

import com.cartagenacorp.lm_oauth.dto.UserDTO;
import com.cartagenacorp.lm_oauth.entity.User;
import com.cartagenacorp.lm_oauth.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Transactional
    public User register(UserDTO userDTO) {
        if (userRepository.findByEmail(userDTO.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The user already exists");
        }
        User user = new User();
        user.setEmail(userDTO.getEmail());
        return userRepository.save(user);
    }

    @Transactional
    public User login(UserDTO userDTO) {
        return userRepository.findByEmail(userDTO.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
    }

    @Transactional(readOnly = true)
    public User authByUUID(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }
}
