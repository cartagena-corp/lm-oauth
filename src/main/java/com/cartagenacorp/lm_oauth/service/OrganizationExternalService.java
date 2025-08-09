package com.cartagenacorp.lm_oauth.service;

import com.cartagenacorp.lm_oauth.exceptions.BaseException;
import com.cartagenacorp.lm_oauth.util.ConstantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
public class OrganizationExternalService {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationExternalService.class);

    @Value("${organization.service.url}")
    private String organizationServiceUrl;

    private final RestTemplate restTemplate;

    public OrganizationExternalService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean organizationExists(UUID organizationId, String token) {
        logger.debug("Validando existencia de la organización con ID: {}", organizationId);
        try {
            String url = organizationServiceUrl + "/organizationExists/" + organizationId;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<Boolean> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    Boolean.class
            );
            boolean exists = Boolean.TRUE.equals(response.getBody());
            logger.info("Resultado de validación de la organización con ID {}: {}", organizationId, exists);
            return exists;
        } catch (HttpClientErrorException.NotFound ex) {
            logger.warn("Organización no encontrada: {}", organizationId);
        } catch (HttpClientErrorException.Unauthorized ex) {
            logger.warn("Token no autorizado al validar la organización con ID {}: {}", organizationId, ex.getMessage());
            throw new BaseException(ConstantUtil.PERMISSION_DENIED, HttpStatus.UNAUTHORIZED.value());
        } catch (HttpClientErrorException.Forbidden ex) {
            logger.warn("No tiene permisos para validar la organización con ID {}: {}", organizationId, ex.getMessage());
            throw new BaseException(ConstantUtil.PERMISSION_DENIED, HttpStatus.UNAUTHORIZED.value());
        } catch (ResourceAccessException ex) {
            logger.warn("El servicio externo no esta disponible: {}",ex.getMessage());
            throw new BaseException(ConstantUtil.ACCESS_EXCEPTION, HttpStatus.SERVICE_UNAVAILABLE.value());
        } catch (Exception ex) {
            logger.error("Error al validar organización con ID {}: {}", organizationId, ex.getMessage(), ex);
            throw new RuntimeException("Error al validar organización con ID: " + organizationId, ex);
        }
        return false;
    }
}
