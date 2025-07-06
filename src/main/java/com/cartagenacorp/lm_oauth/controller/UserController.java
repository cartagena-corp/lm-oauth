package com.cartagenacorp.lm_oauth.controller;

import com.cartagenacorp.lm_oauth.dto.UserDTO;
import com.cartagenacorp.lm_oauth.entity.RefreshToken;
import com.cartagenacorp.lm_oauth.entity.User;
import com.cartagenacorp.lm_oauth.dto.UserDtoResponse;
import com.cartagenacorp.lm_oauth.repository.UserRepository;
import com.cartagenacorp.lm_oauth.service.RefreshTokenService;
import com.cartagenacorp.lm_oauth.service.RoleService;
import com.cartagenacorp.lm_oauth.service.UserService;
import com.cartagenacorp.lm_oauth.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/oauth/")
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
    public ResponseEntity<?> refreshToken(@CookieValue(value = "refreshToken", required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Refresh token missing"));
        }

        return refreshTokenService.findByToken(refreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String newJwt = jwtTokenUtil.generateToken(
                            user.getId().toString(),
                            user.getEmail(),
                            user.getFirstName(),
                            user.getLastName(),
                            user.getPicture(),
                            user.getRole(),
                            roleService.getPermissionsByRole(user.getRole())
                    );
                    return ResponseEntity.ok(Map.of(
                            "accessToken", newJwt
                    ));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid refresh token")));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(value = "refreshToken", required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body("Refresh token not found in cookie");
        }

        boolean deleted = refreshTokenService.deleteByToken(refreshToken);

        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .build();

        return deleted ?
                ResponseEntity.ok()
                        .header("Set-Cookie", deleteCookie.toString())
                        .body("Logged out successfully") :
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .header("Set-Cookie", deleteCookie.toString())
                        .body("Refresh token not found or already invalidated");
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<?> getUserById(@PathVariable String id) {
        UUID uuid = UUID.fromString(id);
        UserDtoResponse user = userService.getUserById(uuid);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/users/batch")
    public ResponseEntity<List<UserDtoResponse>> getUsersByIds(@RequestBody List<String> ids) {
        List<UUID> uuidList = ids.stream().map(UUID::fromString).toList();
        List<UserDtoResponse> users = userService.getUsersByIds(uuidList);
        return ResponseEntity.ok(users);
    }

    @PutMapping("/user/{id}/role")
    public ResponseEntity<?> assignRoleToUser(@PathVariable String id, @RequestBody String roleName) {
        UUID uuid = UUID.fromString(id);
        userService.assignRoleToUser(uuid, roleName);
        return ResponseEntity.ok().build();
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

    @PostMapping("/add-user")
    public ResponseEntity<String> addUser(@RequestBody UserDTO userDTO) {
        userService.addUser(userDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body("User added successfully");
    }

    @PostMapping("/import")
    public ResponseEntity<String> importUsersFromExcel(@RequestParam("file") MultipartFile file) {
        try {
            userService.importUsers(file);
            return ResponseEntity.ok("Users imported successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error importing users: " + e.getMessage());
        }
    }
}
