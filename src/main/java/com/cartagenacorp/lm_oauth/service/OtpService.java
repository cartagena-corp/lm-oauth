package com.cartagenacorp.lm_oauth.service;

import com.cartagenacorp.lm_oauth.dto.NotificationResponse;
import com.cartagenacorp.lm_oauth.dto.OtpRequest;
import com.cartagenacorp.lm_oauth.dto.OtpResponse;
import com.cartagenacorp.lm_oauth.dto.RegisterRequestDto;
import com.cartagenacorp.lm_oauth.entity.Otp;
import com.cartagenacorp.lm_oauth.entity.OtpFunctionality;
import com.cartagenacorp.lm_oauth.exceptions.BaseException;
import com.cartagenacorp.lm_oauth.repository.OtpFunctionalityRepository;
import com.cartagenacorp.lm_oauth.repository.OtpRepository;
import com.cartagenacorp.lm_oauth.repository.UserRepository;
import com.cartagenacorp.lm_oauth.util.CryptoUtil;
import com.cartagenacorp.lm_oauth.util.HashUtil;
import com.cartagenacorp.lm_oauth.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);

    private static final SecureRandom random = new SecureRandom();
    private static final int OTP_LENGTH = 6;

    private final OtpRepository otpRepository;
    private final OtpFunctionalityRepository otpFunctionalityRepository;
    private final EmailService emailService;
    private final UserRepository userRepository;

    public OtpService(OtpRepository otpRepository, OtpFunctionalityRepository otpFunctionalityRepository,
                      EmailService emailService, UserRepository userRepository) {
        this.otpRepository = otpRepository;
        this.otpFunctionalityRepository = otpFunctionalityRepository;
        this.emailService = emailService;
        this.userRepository = userRepository;
    }

    @Transactional
    public OtpResponse generateOtp(OtpRequest otpRequest){
        logger.info("=== [OtpService] Iniciando flujo de generación de OTP ===");

        if(!userRepository.existsByEmail(otpRequest.getRegisterRequestDto().getEmail())){
            throw new BaseException("El usuario no está autorizado. Contacta al administrador.", HttpStatus.UNAUTHORIZED.value());
        }

        OtpFunctionality otpFunctionality = otpFunctionalityRepository.findByName(otpRequest.getFunctionality().getName())
                .orElseThrow(() -> new BaseException("Funcionalidad OTP no encontrada", HttpStatus.NOT_FOUND.value()));

        otpRepository.deactivateAllActiveOtps(otpRequest.getRegisterRequestDto().getEmail());

        RegisterRequestDto original = otpRequest.getRegisterRequestDto();
        RegisterRequestDto safeRegisterRequest = RegisterRequestDto.builder()
                .email(original.getEmail())
                .firstName(original.getFirstName())
                .lastName(original.getLastName())
                .password("****")
                .build();

        String codeGenerated = generateOtp();
        String operationBody = safeRegisterRequest.toString() + "," + otpRequest.getFunctionality();
        String objectHash = HashUtil.generateHash(operationBody);

        String passphrase = CryptoUtil.generatePassphrase();
        String passphraseMD5 = CryptoUtil.md5(passphrase);

        otpRepository.save(Otp.builder()
                .code(codeGenerated)
                .passphrase(passphrase)
                .object(operationBody)
                .hashObject(objectHash)
                .attempt(0)
                .active(true)
                .created(LocalDateTime.now())
                .email(otpRequest.getRegisterRequestDto().getEmail())
                .otpFunctionality(otpFunctionality)
                .build()
        );

        try {
            emailService.sendOtpEmail(otpRequest.getRegisterRequestDto().getEmail(), codeGenerated);
        } catch (Exception ex) {
            throw new BaseException("Error al enviar email", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        NotificationResponse notificationResponse = ResponseUtil.success("OTP generada, se ha enviado al email registrado", HttpStatus.OK);

        logger.info("=== [OtpService] Flujo de generación de OTP finalizado correctamente ===");
        return  OtpResponse.builder().passphrase(passphrase).notificationResponse(notificationResponse).build();
    }

    public void validateOtp(OtpRequest otpRequest){
        logger.info("=== [OtpService] Iniciando flujo de validación de OTP ===");

        OtpFunctionality otpFunctionality = otpFunctionalityRepository.findByName(otpRequest.getFunctionality().getName())
                .orElseThrow(() -> new BaseException("Funcionalidad OTP no encontrada", HttpStatus.NOT_FOUND.value()));

        RegisterRequestDto original = otpRequest.getRegisterRequestDto();
        RegisterRequestDto safeRegisterRequest = RegisterRequestDto.builder()
                .email(original.getEmail())
                .firstName(original.getFirstName())
                .lastName(original.getLastName())
                .password("****")
                .build();

        String otpEncrypted = otpRequest.getCode();
        String operationBody = safeRegisterRequest.toString() + "," + otpRequest.getFunctionality();
        String objectHash = HashUtil.generateHash(operationBody);

        Otp otp = otpRepository.findFirstByHashObjectAndActiveOrderByCreatedDesc(
                objectHash,
                true
        ).orElseThrow(() -> new BaseException("OTP no disponible", HttpStatus.NOT_FOUND.value()));

        otp.setAttempt(otp.getAttempt() + 1);
        otpRepository.save(otp);

        isValidAttemps(otp, otpFunctionality);
        integrityOtp(otp, objectHash);

        String passphrase = otp.getPassphrase();
        String passphraseMD5 = CryptoUtil.md5(passphrase);

        String otpDecrypted = CryptoUtil.decrypt(otpEncrypted, passphraseMD5);

        if (!otp.getCode().equals(otpDecrypted)) {
            throw new BaseException("OTP inválida", HttpStatus.BAD_REQUEST.value());
        }

        otp.setActive(false);
        otpRepository.save(otp);
        logger.info("=== [OtpService] Flujo de validación de OTP finalizado correctamente ===");
    }

    private void integrityOtp(Otp otp, String hashObject) {
        if (!otp.getHashObject().equals(hashObject)) {
            otp.setActive(false);
            otpRepository.save(otp);
            throw new BaseException("Error de integridad de los datos", HttpStatus.CONFLICT.value());
        }
    }

    private void isValidAttemps(Otp otp, OtpFunctionality otpFunctionality) {
        LocalDateTime expirationTime = otp.getCreated().plusSeconds(otpFunctionality.getTimeToLive());

        if (otp.getAttempt() >= otpFunctionality.getAttemptLimit()) {
            otp.setActive(false);
            otpRepository.save(otp);
            throw new BaseException("Ha sobrepasado el número de intentos por favor genere nuevamente la OTP", HttpStatus.TOO_MANY_REQUESTS.value());
        }

        if (LocalDateTime.now().isAfter(expirationTime)) {
            otp.setActive(false);
            otpRepository.save(otp);
            throw new BaseException("La OTP ha expirado, por favor genere una nueva", HttpStatus.UNPROCESSABLE_ENTITY.value());
        }
    }

    public String generateOtp() {
        String digits = "0123456789";
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(digits.charAt(random.nextInt(digits.length())));
        }
        return otp.toString();
    }
}
