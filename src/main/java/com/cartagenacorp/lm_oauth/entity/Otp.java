package com.cartagenacorp.lm_oauth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Otp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "passphrase", nullable = false)
    private String passphrase;

    @Column(name = "object", nullable = false)
    private String object;

    @Column(name = "hash_object", nullable = false)
    private String hashObject;

    @Column(name = "attempt", nullable = false)
    private Integer attempt;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created", nullable = false)
    private LocalDateTime created;

    @Column(name = "email", nullable = false)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="functionality_id", nullable=false)
    private OtpFunctionality otpFunctionality;
}
