package com.cartagenacorp.lm_oauth.controller;

import com.cartagenacorp.lm_oauth.dto.UserDTO;
import com.cartagenacorp.lm_oauth.entity.User;
import com.cartagenacorp.lm_oauth.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/oauth/users")
@CrossOrigin(origins = "*")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserDTO userDTO) {
        try {
            return ResponseEntity.ok(userService.register(userDTO));
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getReason());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserDTO userDTO) {
        try {
            return ResponseEntity.ok(userService.login(userDTO));
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getReason());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred");
        }
    }

    @GetMapping("/auth/{userId}")
    public ResponseEntity<?> authByUUID(@PathVariable UUID userId) {
        try {
            return ResponseEntity.ok(userService.authByUUID(userId));
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred");
        }
    }
}
