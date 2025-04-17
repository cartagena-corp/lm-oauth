package com.cartagenacorp.lm_oauth.service;

import com.cartagenacorp.lm_oauth.entity.User;
import com.cartagenacorp.lm_oauth.dto.UserDtoResponse;
import com.cartagenacorp.lm_oauth.mapper.UserMapper;
import com.cartagenacorp.lm_oauth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Autowired
    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    public void assignRoleToUser(UUID userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setRole(roleName);
        userRepository.save(user);
    }

    public UserDtoResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return userMapper.toDto(user);
    }

    public List<UserDtoResponse> getUsersByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<User> users = userRepository.findAllById(ids);
        return users.stream().map(userMapper::toDto).toList();
    }
}
