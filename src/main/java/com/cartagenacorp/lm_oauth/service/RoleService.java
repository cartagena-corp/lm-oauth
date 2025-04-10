package com.cartagenacorp.lm_oauth.service;

import com.cartagenacorp.lm_oauth.dto.PermissionDTO;
import com.cartagenacorp.lm_oauth.dto.RoleDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class RoleService {
    @Value("${role.service.url}")
    private String roleServiceUrl;

    private final RestTemplate restTemplate;

    @Autowired
    public RoleService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<String> getPermissionsByRole(String role) {
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
            System.out.println("Role not found: " + role);
        } catch (Exception ex) {
            System.out.println("Error obtaining permissions: " + ex.getMessage());
        }
        return Collections.emptyList();
    }
}
