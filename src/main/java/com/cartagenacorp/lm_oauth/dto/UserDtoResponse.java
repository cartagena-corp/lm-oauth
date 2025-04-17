package com.cartagenacorp.lm_oauth.dto;

import com.cartagenacorp.lm_oauth.entity.User;
import lombok.Value;

import java.io.Serializable;
import java.util.UUID;

/**
 * DTO for {@link User}
 */
@Value
public class UserDtoResponse implements Serializable {
    UUID id;
    String firstName;
    String lastName;
    String picture;
}