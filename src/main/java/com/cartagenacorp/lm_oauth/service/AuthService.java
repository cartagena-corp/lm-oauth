package com.cartagenacorp.lm_oauth.service;

import com.cartagenacorp.lm_oauth.dto.LoginRequestDto;
import com.cartagenacorp.lm_oauth.dto.OtpRequest;
import com.cartagenacorp.lm_oauth.dto.RegisterRequestDto;
import com.cartagenacorp.lm_oauth.entity.RefreshToken;
import com.cartagenacorp.lm_oauth.entity.User;
import com.cartagenacorp.lm_oauth.exceptions.BaseException;
import com.cartagenacorp.lm_oauth.repository.UserRepository;
import com.cartagenacorp.lm_oauth.util.JwtTokenUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Value("${app.jwt.refreshExpiration}")
    private long refreshExpirationMs;

    private final JwtTokenUtil jwtTokenUtil;
    private final UserRepository userRepository;
    private final RoleExternalService roleExternalService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    public AuthService(JwtTokenUtil jwtTokenUtil, UserRepository userRepository,
                       RoleExternalService roleExternalService, RefreshTokenService refreshTokenService,
                       PasswordEncoder passwordEncoder, OtpService otpService) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.userRepository = userRepository;
        this.roleExternalService = roleExternalService;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
    }

    public void registerUser(OtpRequest otpRequest) {
        logger.info("=== [AuthService] Iniciando flujo de registro de usuario ===");

        RegisterRequestDto registerRequestDto = otpRequest.getRegisterRequestDto();

        logger.info("[AuthService] Verificando si el usuario con email {} esta autorizado para registrar sus datos", registerRequestDto.getEmail());
        User user = userRepository.findByEmail(registerRequestDto.getEmail())
                .orElseThrow(() -> {
                    logger.warn("[AuthService] Usuario con email {} no está autorizado para registrar sus datos", registerRequestDto.getEmail());
                    return new BaseException("El usuario no está autorizado. Contacta al administrador.",
                            HttpStatus.UNAUTHORIZED.value());
                });

        logger.info("[AuthService] Verificando si el usuario con email {} ya completó su registro", registerRequestDto.getEmail());
        if (user.isRegistered()) {
            logger.warn("[AuthService] El usuario con email {} ya completó su registro", registerRequestDto.getEmail());
            throw new BaseException("El usuario ya completó su registro", HttpStatus.BAD_REQUEST.value());
        }

        logger.info("[AuthService] Usuario con email {} no ha completado su registro, procediendo a validar OTP", registerRequestDto.getEmail());
        otpService.validateOtp(otpRequest);

        logger.debug("[AuthService] OTP validado, actualizando datos del usuario con email {}", registerRequestDto.getEmail());
        user.setFirstName(registerRequestDto.getFirstName());
        user.setLastName(registerRequestDto.getLastName());
        user.setPassword(passwordEncoder.encode(registerRequestDto.getPassword()));
        user.setRegistered(true);

        userRepository.save(user);
        logger.debug("[AuthService] Datos del usuario con email {} actualizados correctamente", registerRequestDto.getEmail());
        logger.info("=== [AuthService] Flujo de registro de usuario finalizado correctamente ===");
    }

    public String authenticateUser(LoginRequestDto loginRequestDTO, HttpServletResponse response) {
        logger.info("=== [AuthService] Inicio flujo de inicio de sesión con contraseña ===");

        logger.info("[AuthService] Verificando si el usuario con email {} está autorizado para iniciar sesión", loginRequestDTO.getEmail());
        User user = userRepository.findByEmail(loginRequestDTO.getEmail())
                .orElseThrow(() -> {
                    logger.warn("[AuthService] Usuario con email {} no está autorizado para iniciar sesión", loginRequestDTO.getEmail());
                    return new BaseException("El usuario no está autorizado. Contacta al administrador.",
                            HttpStatus.UNAUTHORIZED.value());
                });

        logger.info("[AuthService] Verificando si el usuario con email {} ha completado su registro", loginRequestDTO.getEmail());
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            logger.warn("[AuthService] Usuario con email {} no tiene contraseña asignada", loginRequestDTO.getEmail());
            throw new BaseException("El usuario no tiene contraseña asignada. Use login con Google o complete su registro.", HttpStatus.UNAUTHORIZED.value());
        }

        logger.info("[AuthService] Verificando credenciales del usuario con email {}", loginRequestDTO.getEmail());
        if (!passwordEncoder.matches(loginRequestDTO.getPassword(), user.getPassword())) {
            logger.warn("[AuthService] Credenciales inválidas para el usuario con email {}", loginRequestDTO.getEmail());
            throw new BaseException("Credenciales inválidas", HttpStatus.UNAUTHORIZED.value());
        }

        String role = user.getRole();
        UUID organizationId = user.getOrganizationId();

        logger.info("[AuthService] Solicitando permisos al servicio externo para role={} organizationId={}", role, organizationId);
        List<String> permissions = roleExternalService.getPermissionsByRole(role, organizationId);
        logger.debug("[AuthService] Permisos obtenidos: {}", permissions);

        logger.info("[AuthService] Generando JWT para usuario email={}", loginRequestDTO.getEmail());
        String accessToken = jwtTokenUtil.generateToken(
                user.getId().toString(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPicture(),
                role,
                permissions,
                organizationId
        );

        logger.info("[AuthService] Creando refresh token para usuario email={}", loginRequestDTO.getEmail());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken.getToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(refreshExpirationMs / 1000)
                .sameSite("Strict")
                .build();

        response.setHeader("Set-Cookie", cookie.toString());
        logger.info("[AuthService] Refresh token añadido a la cookie");

        logger.info("=== [AuthService]  Flujo de inicio de sesión con contraseña finalizado correctamente ===");
        return accessToken;
    }
}
