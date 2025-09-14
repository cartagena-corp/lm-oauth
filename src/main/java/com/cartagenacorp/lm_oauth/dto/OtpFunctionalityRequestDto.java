package com.cartagenacorp.lm_oauth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OtpFunctionalityRequestDto {
    @NotBlank(message = "El nombre de la funcionalidad es obligatorio")
    private String name;
}
