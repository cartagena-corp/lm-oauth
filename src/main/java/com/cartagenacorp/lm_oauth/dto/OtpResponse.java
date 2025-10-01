package com.cartagenacorp.lm_oauth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OtpResponse {
    private String passphrase;
    private NotificationResponse notificationResponse;
}
