package com.cartagenacorp.lm_oauth.repository;

import com.cartagenacorp.lm_oauth.entity.OtpFunctionality;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpFunctionalityRepository extends JpaRepository<OtpFunctionality, Long> {
    Optional<OtpFunctionality> findByName(String name);
}