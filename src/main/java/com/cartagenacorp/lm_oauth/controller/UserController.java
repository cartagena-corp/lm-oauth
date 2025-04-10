package com.cartagenacorp.lm_oauth.controller;

import com.cartagenacorp.lm_oauth.entity.User;
import com.cartagenacorp.lm_oauth.repository.UserRepository;
import com.cartagenacorp.lm_oauth.service.UserService;
import com.cartagenacorp.lm_oauth.util.JwtTokenUtil;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/oauth/")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtTokenUtil jwtTokenUtil;

    @Autowired
    public UserController(UserRepository userRepository, UserService userService, JwtTokenUtil jwtTokenUtil) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @GetMapping("/validate/{userId}")
    public ResponseEntity<Boolean> validateUser(@PathVariable UUID userId, @RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtTokenUtil.validateToken(token)) {
                boolean exists = userRepository.existsById(userId);
                return ResponseEntity.ok(exists);
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
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

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers(@RequestHeader("Authorization") String authHeader){
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtTokenUtil.validateToken(token)) {
                List<User> users = userRepository.findAll();
                return ResponseEntity.ok(users);
            }
        }
        return ResponseEntity.badRequest().build();
    }

    @PutMapping("/user/{id}/role")
    public ResponseEntity<?> assignRoleToUser(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String id,
            @RequestBody String roleName) {

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtTokenUtil.validateToken(token)) {
                try {
                    UUID uuid = UUID.fromString(id);
                    userService.assignRoleToUser(uuid, roleName);
                    return ResponseEntity.ok().build();
                } catch (ResponseStatusException ex) {
                    return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason());
                } catch (IllegalArgumentException ex){
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
                } catch (DataIntegrityViolationException | ConstraintViolationException ex) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body("The role could not be assigned because it does not exist or is incorrectly related");
                } catch (Exception ex){
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred");
                }
            }
        }
        return ResponseEntity.badRequest().build();
    }
}
