package com.cartagenacorp.lm_oauth.repository;

import com.cartagenacorp.lm_oauth.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {

    Optional<Otp> findFirstByHashObjectAndActiveOrderByCreatedDesc(
            String hashObject,
            Boolean active
    );

    @Modifying
    @Query("UPDATE Otp o SET o.active = false WHERE o.email = ?1 AND o.active = true")
    void deactivateAllActiveOtps(
            String email
    );
}