package com.cartagenacorp.lm_oauth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Value("${aws.ses.sender-email}")
    private String senderEmail;

    private final SesClient sesClient;

    public EmailService(SesClient sesClient) {
        this.sesClient = sesClient;
    }

    public void sendEmail(String recipientEmail, String subject, String bodyHtml, String bodyText) {
        try {
            logger.info("=== [EmailService] Iniciando flujo de envio de email ===");
            Content subjectContent = Content.builder()
                    .data(subject)
                    .charset("UTF-8")
                    .build();

            Content bodyHtmlContent = Content.builder()
                    .data(bodyHtml)
                    .charset("UTF-8")
                    .build();

            Content bodyTextContent = Content.builder()
                    .data(bodyText)
                    .charset("UTF-8")
                    .build();

            Body emailBody = Body.builder()
                    .html(bodyHtmlContent)
                    .text(bodyTextContent)
                    .build();

            Message message = Message.builder()
                    .subject(subjectContent)
                    .body(emailBody)
                    .build();

            SendEmailRequest sendEmailRequest = SendEmailRequest.builder()
                    .source(senderEmail)
                    .destination(Destination.builder()
                            .toAddresses(recipientEmail)
                            .build())
                    .message(message)
                    .build();

            logger.info("[EmailService] Enviando email via AWS SES a {}", recipientEmail);
            SendEmailResponse response = sesClient.sendEmail(sendEmailRequest);
            logger.info("[EmailService] Email enviado via AWS SES con ID: {}", response.messageId());
            logger.info("=== [EmailService] Flujo de envio de email finalizado correctamente ===");
        } catch (SesException e) {
            logger.error("[EmailService] Error al enviar email via AWS SES: {}", e.awsErrorDetails().errorMessage());
            throw e;
        }
    }

    public void sendOtpEmail(String recipientEmail, String otp) {
        String subject = "Tu código de verificación";
        String bodyHtml = String.format(
                "<h1>Código de Verificación</h1><p>Tu código de un solo uso (OTP) es: <strong>%s</strong></p><p>Este código expirará pronto.</p>",
                otp
        );
        String bodyText = String.format(
                "Código de Verificación\nTu código de un solo uso (OTP) es: %s\nEste código expirará pronto.",
                otp
        );
        sendEmail(recipientEmail, subject, bodyHtml, bodyText);
    }

    public void shutdown() {
        sesClient.close();
    }
}
