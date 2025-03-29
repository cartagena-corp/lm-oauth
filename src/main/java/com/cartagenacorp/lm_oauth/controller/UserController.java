package com.cartagenacorp.lm_oauth.controller;

import com.cartagenacorp.lm_oauth.dto.UserDTO;
import com.cartagenacorp.lm_oauth.entity.User;
import com.cartagenacorp.lm_oauth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/oauth/users")
@CrossOrigin(origins = "*")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody UserDTO userDTO) {
        return ResponseEntity.ok(userService.register(userDTO));
    }

    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestBody UserDTO userDTO) {
        return ResponseEntity.ok(userService.login(userDTO));
    }
}
