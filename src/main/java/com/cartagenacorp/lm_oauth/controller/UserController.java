package com.cartagenacorp.lm_oauth.controller;

import com.cartagenacorp.lm_oauth.dto.NotificationResponse;
import com.cartagenacorp.lm_oauth.dto.PageResponseDTO;
import com.cartagenacorp.lm_oauth.dto.UserDTO;
import com.cartagenacorp.lm_oauth.entity.RefreshToken;
import com.cartagenacorp.lm_oauth.entity.User;
import com.cartagenacorp.lm_oauth.dto.UserDtoResponse;
import com.cartagenacorp.lm_oauth.repository.UserRepository;
import com.cartagenacorp.lm_oauth.service.RefreshTokenService;
import com.cartagenacorp.lm_oauth.service.RoleExternalService;
import com.cartagenacorp.lm_oauth.service.UserService;
import com.cartagenacorp.lm_oauth.util.ConstantUtil;
import com.cartagenacorp.lm_oauth.util.JwtTokenUtil;
import com.cartagenacorp.lm_oauth.util.ResponseUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final RoleExternalService roleExternalService;

    public UserController(UserRepository userRepository, UserService userService, JwtTokenUtil jwtTokenUtil,
                          RefreshTokenService refreshTokenService, RoleExternalService roleExternalService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.refreshTokenService = refreshTokenService;
        this.roleExternalService = roleExternalService;
    }

    @GetMapping("/validate/{userId}")
    public ResponseEntity<Boolean> validateUser(@PathVariable UUID userId) {
        Boolean exists = userService.validateUser(userId);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/token")
    public ResponseEntity<UUID> getUserIdFromToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();
        return ResponseEntity.ok(user.getId());
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
                            roleExternalService.getPermissionsByRole(user.getRole()),
                            user.getOrganizationId()
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
    public ResponseEntity<PageResponseDTO<UserDtoResponse>> getAllUsers(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponseDTO<UserDtoResponse> result = userService.searchUsers(search, page, size);
        return ResponseEntity.ok(result);
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
    @PreAuthorize("hasAnyAuthority('ROLE_CRUD', 'ROLE_UPDATE')")
    public ResponseEntity<NotificationResponse> assignRoleToUser(@PathVariable String id, @RequestBody String roleName) {
        UUID uuid = UUID.fromString(id);
        userService.assignRoleToUser(uuid, roleName);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ResponseUtil.success(ConstantUtil.Success.ROLE_ASSIGNED, HttpStatus.OK));
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
    @PreAuthorize("hasAnyAuthority('USER_CREATE_WITH_MY_ORGANIZATION')")
    public ResponseEntity<NotificationResponse> addUser(@RequestBody UserDTO userDTO) {
        userService.addUser(userDTO);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ResponseUtil.success(ConstantUtil.Success.USER_CREATED, HttpStatus.CREATED));
    }

    @PostMapping("/add-user-with-organization")
    @PreAuthorize("hasAnyAuthority('USER_CREATE_WITH_OTHER_ORGANIZATION')")
    public ResponseEntity<NotificationResponse> addUserWithOrganization(@RequestBody UserDTO userDTO) {
        userService.addUserWithOrganization(userDTO);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ResponseUtil.success(ConstantUtil.Success.USER_CREATED, HttpStatus.CREATED));
    }

    @PostMapping("/import")
    @PreAuthorize("hasAnyAuthority('USER_CREATE_WITH_MY_ORGANIZATION')")
    public ResponseEntity<NotificationResponse> importUsersFromExcel(@RequestParam("file") MultipartFile file) {
        userService.importUsers(file);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ResponseUtil.success(ConstantUtil.Success.USERS_IMPORT, HttpStatus.OK));
    }
}
