package com.cartagenacorp.lm_oauth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private static final String COMPANY_NAME = "La Muralla";

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

    public void sendOtpEmail(String recipientEmail, String otp) throws IOException {
        String subject = "Tu código de verificación";

        String bodyHtml;
        try {
            bodyHtml = loadTemplate("static/otp-template.html")
                    .replace("{{OTP}}", otp)
                    .replace("{{COMPANY}}", COMPANY_NAME);
        } catch (IOException e) {
            logger.error("[EmailService] Error al cargar la plantilla de email: {}", e.getMessage());
            throw e;
        }

        String bodyText = String.format(
                "Código de Verificación\nTu código de un solo uso (OTP) es: %s\nExpira en 2 minutos.\n%s",
                otp, COMPANY_NAME
        );

        sendEmail(recipientEmail, subject, bodyHtml, bodyText);
    }

    private String loadTemplate(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    public void shutdown() {
        sesClient.close();
    }
}
