package com.cartagenacorp.lm_oauth.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "otp_functionality")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OtpFunctionality {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", unique = true, nullable = false)
    private String name; //unico por funcionalidad

    @Column(name = "time_to_live", nullable = false)
    private Integer timeToLive;

    @Column(name = "attempt_limit", nullable = false)
    private Integer attemptLimit;
}
