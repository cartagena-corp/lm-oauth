package com.cartagenacorp.lm_oauth.controller;

import com.cartagenacorp.lm_oauth.dto.LoginRequestDto;
import com.cartagenacorp.lm_oauth.dto.NotificationResponse;
import com.cartagenacorp.lm_oauth.dto.OtpRequest;
import com.cartagenacorp.lm_oauth.service.AuthService;
import com.cartagenacorp.lm_oauth.util.ResponseUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Value("${app.oauth2.redirect-url}")
    private String oauth2RedirectUrl;

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<NotificationResponse> register(@Valid @RequestBody OtpRequest otpRequest) {
        authService.registerUser(otpRequest);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ResponseUtil.success("Registro completado", HttpStatus.OK));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto loginRequestDto, HttpServletResponse response) throws IOException {
        String accessToken = authService.authenticateUser(loginRequestDto, response);
        return ResponseEntity.ok().body(Map.of("accessToken", accessToken));
    }
}
