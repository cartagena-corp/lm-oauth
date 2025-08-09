package com.cartagenacorp.lm_oauth.service;

import com.cartagenacorp.lm_oauth.dto.PermissionDTO;
import com.cartagenacorp.lm_oauth.dto.RoleDTO;
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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class RoleExternalService {

    private static final Logger logger = LoggerFactory.getLogger(RoleExternalService.class);

    @Value("${role.service.url}")
    private String roleServiceUrl;

    private final RestTemplate restTemplate;

    public RoleExternalService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<String> getPermissionsByRole(String role) {
        logger.debug("Validando los permisos del rol: {}", role);
        if (role == null) {
            return Collections.emptyList();
        }
        try {
            String url = roleServiceUrl + "/" + role;
            ResponseEntity<RoleDTO> response = restTemplate.getForEntity(url, RoleDTO.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody().getPermissions().stream()
                        .map(PermissionDTO::getName)
                        .collect(Collectors.toList());
            }
        } catch (HttpClientErrorException.NotFound ex) {
            logger.warn("Rol no encontrado: {}", role);
        } catch (ResourceAccessException ex) {
            logger.warn("El servicio externo no esta disponible: {}",ex.getMessage());
            throw new BaseException(ConstantUtil.ACCESS_EXCEPTION, HttpStatus.SERVICE_UNAVAILABLE.value());
        } catch (Exception ex) {
            logger.error("Error al obtener los permisos del rol {}: {}", role, ex.getMessage(), ex);
            throw new RuntimeException("Error al obtener los permisos del rol: " + role, ex);
        }
        return Collections.emptyList();
    }

    public boolean roleExists(String role, String token) {
        logger.debug("Validando existencia del rol: {}", role);
        try {
            String url = roleServiceUrl + "/exists/" + role;

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
            logger.info("Resultado de validación del rol {}: {}", role, exists);
            return exists;
        } catch (HttpClientErrorException.NotFound ex) {
            logger.warn("Rol no encontrado: {}", role);
        } catch (HttpClientErrorException.Unauthorized ex) {
            logger.warn("Token no autorizado al validar el rol {}: {}", role, ex.getMessage());
            throw new BaseException(ConstantUtil.PERMISSION_DENIED, HttpStatus.UNAUTHORIZED.value());
        } catch (HttpClientErrorException.Forbidden ex) {
            logger.warn("No tiene permisos para validar el rol {}: {}", role, ex.getMessage());
            throw new BaseException(ConstantUtil.PERMISSION_DENIED, HttpStatus.UNAUTHORIZED.value());
        } catch (ResourceAccessException ex) {
            logger.warn("El servicio externo no esta disponible: {}",ex.getMessage());
            throw new BaseException(ConstantUtil.ACCESS_EXCEPTION, HttpStatus.SERVICE_UNAVAILABLE.value());
        } catch (Exception ex) {
            logger.error("Error al validar el rol {}: {}", role, ex.getMessage(), ex);
            throw new RuntimeException("Error al validar el rol: " + role, ex);
        }
        return false;
    }
}
