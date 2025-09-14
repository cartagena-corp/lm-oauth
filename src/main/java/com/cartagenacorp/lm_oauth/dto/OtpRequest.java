package com.cartagenacorp.lm_oauth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO for {@link com.cartagenacorp.lm_oauth.entity.Otp}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OtpRequest implements Serializable {
    private String code;

    @Valid
    @NotNull(message = "Los datos de registro son obligatorios")
    private RegisterRequestDto registerRequestDto;

    @Valid
    @NotNull(message = "Los datos de la funcionalidad son obligatorios")
    private OtpFunctionalityRequestDto functionality;
}