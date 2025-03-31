package com.cartagenacorp.lm_oauth.controller;

import com.cartagenacorp.lm_oauth.repository.UserRepository;
import com.cartagenacorp.lm_oauth.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/oauth/")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @GetMapping("/validate/{userId}")
    public ResponseEntity<Boolean> validateUser(@PathVariable UUID userId) {
        boolean exists = userRepository.existsById(userId);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/token")
    public ResponseEntity<String> getUserIdFromToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtTokenUtil.validateToken(token)) {
                UUID userId = jwtTokenUtil.getUserUUIDFromToken(token);
                return ResponseEntity.ok(userId.toString());
            }
        }

        return ResponseEntity.badRequest().build();
    }
}
