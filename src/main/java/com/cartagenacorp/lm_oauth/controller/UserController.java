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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
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

    @GetMapping("/users/resolve")
    public ResponseEntity<UUID> resolveUser(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String identifier) {

        if (identifier == null || identifier.isBlank()) {
            return ResponseEntity.badRequest().body(null);
        }

        String decodedIdentifier = URLDecoder.decode(identifier, StandardCharsets.UTF_8);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtTokenUtil.validateToken(token)) {

                String lower = decodedIdentifier.trim().toLowerCase();

                Optional<User> userOpt = userRepository.findAll().stream()
                        .filter(user -> {
                            String email = user.getEmail() != null ? user.getEmail().trim().toLowerCase() : "";
                            String fullNameDot = (user.getFirstName() != null && user.getLastName() != null)
                                    ? (user.getFirstName().trim().toLowerCase() + "." + user.getLastName().trim().toLowerCase())
                                    : "";
                            String fullNameSpace = (user.getFirstName() != null && user.getLastName() != null)
                                    ? (user.getFirstName().trim().toLowerCase() + " " + user.getLastName().trim().toLowerCase())
                                    : "";
                            String firstName = user.getFirstName() != null ? user.getFirstName().trim().toLowerCase() : "";
                            String lastName = user.getLastName() != null ? user.getLastName().trim().toLowerCase() : "";


                            return lower.equals(email)
                                    || lower.equals(fullNameDot)
                                    || lower.equals(fullNameSpace)
                                    || lower.equals(firstName)
                                    || lower.equals(lastName);
                        })
                        .findFirst();

                return userOpt.map(user -> ResponseEntity.ok(user.getId()))
                        .orElse(ResponseEntity.notFound().build());
            }
        }
        return ResponseEntity.badRequest().build();
    }
}
