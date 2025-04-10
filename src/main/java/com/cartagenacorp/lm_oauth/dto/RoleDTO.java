package com.cartagenacorp.lm_oauth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleDTO {
    private String name;
    private List<PermissionDTO> permissions;
}
