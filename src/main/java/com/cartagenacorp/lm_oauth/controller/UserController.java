package com.cartagenacorp.lm_oauth.controller;

import com.cartagenacorp.lm_oauth.entity.RefreshToken;
import com.cartagenacorp.lm_oauth.entity.User;
import com.cartagenacorp.lm_oauth.repository.UserRepository;
import com.cartagenacorp.lm_oauth.service.RefreshTokenService;
import com.cartagenacorp.lm_oauth.service.RoleService;
import com.cartagenacorp.lm_oauth.service.UserService;
import com.cartagenacorp.lm_oauth.util.JwtTokenUtil;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/oauth/")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtTokenUtil jwtTokenUtil;
    private final RefreshTokenService refreshTokenService;
    private final RoleService roleService;

    @Autowired
    public UserController(UserRepository userRepository, UserService userService, JwtTokenUtil jwtTokenUtil,
                          RefreshTokenService refreshTokenService, RoleService roleService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.refreshTokenService = refreshTokenService;
        this.roleService = roleService;
    }

    @GetMapping("/validate/{userId}")
    public ResponseEntity<Boolean> validateUser(@PathVariable UUID userId) {
        boolean exists = userRepository.existsById(userId);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/token")
    public ResponseEntity<String> getUserIdFromToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();
        return ResponseEntity.ok(user.getId().toString());
    }

    @GetMapping("/validate/token")
    public ResponseEntity<Boolean> validateToken() {
        return ResponseEntity.ok(true);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody String refreshToken) {
        return refreshTokenService.findByToken(refreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String newJwt = jwtTokenUtil.generateToken(
                            user.getId().toString(),
                            user.getEmail(),
                            user.getPicture(),
                            user.getRole(),
                            roleService.getPermissionsByRole(user.getRole())
                    );
                    return ResponseEntity.ok(Map.of(
                            "accessToken", newJwt,
                            "refreshToken", refreshToken
                    ));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid refresh token")));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody String refreshToken) {
        boolean deleted = refreshTokenService.deleteByToken(refreshToken);
        return deleted ?
                ResponseEntity.ok("Logged out successfully") :
                ResponseEntity.status(HttpStatus.NOT_FOUND).body("Refresh token not found or already invalidated");
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PutMapping("/user/{id}/role")
    public ResponseEntity<?> assignRoleToUser(@PathVariable String id, @RequestBody String roleName) {
        try {
            UUID uuid = UUID.fromString(id);
            userService.assignRoleToUser(uuid, roleName);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason());
        } catch (IllegalArgumentException ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid user ID format");
        } catch (DataIntegrityViolationException | ConstraintViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("The role could not be assigned because it does not exist or is incorrectly related");
        } catch (Exception ex){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred");
        }
    }

    @GetMapping("/users/resolve")
    public ResponseEntity<UUID> resolveUser(@RequestParam(required = false) String identifier) {

        if (identifier == null || identifier.isBlank()) {
            return ResponseEntity.badRequest().body(null);
        }

        String decodedIdentifier = URLDecoder.decode(identifier, StandardCharsets.UTF_8);
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
