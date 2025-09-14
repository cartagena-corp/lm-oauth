package com.cartagenacorp.lm_oauth.controller;

import com.cartagenacorp.lm_oauth.dto.NotificationResponse;
import com.cartagenacorp.lm_oauth.dto.OtpRequest;
import com.cartagenacorp.lm_oauth.dto.OtpResponse;
import com.cartagenacorp.lm_oauth.service.OtpService;
import jakarta.validation.Valid;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/otp")
public class OtpController {

    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/generate")
    public HttpEntity<NotificationResponse> generateOTP(@Valid @RequestBody OtpRequest request){
        OtpResponse otpResponse =  otpService.generateOtp(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("X-PHRASE", otpResponse.getPassphrase())
                .body(otpResponse.getNotificationResponse());
    }
}
