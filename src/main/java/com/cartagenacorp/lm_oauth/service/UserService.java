package com.cartagenacorp.lm_oauth.service;

import com.cartagenacorp.lm_oauth.dto.PageResponseDTO;
import com.cartagenacorp.lm_oauth.dto.UserDTO;
import com.cartagenacorp.lm_oauth.entity.User;
import com.cartagenacorp.lm_oauth.dto.UserDtoResponse;
import com.cartagenacorp.lm_oauth.exceptions.BaseException;
import com.cartagenacorp.lm_oauth.mapper.UserMapper;
import com.cartagenacorp.lm_oauth.repository.UserRepository;
import com.cartagenacorp.lm_oauth.util.ConstantUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RoleExternalService roleExternalService;
    private final OrganizationExternalService organizationExternalService;

    public UserService(UserRepository userRepository, UserMapper userMapper,
                       RoleExternalService roleExternalService, OrganizationExternalService organizationExternalService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.roleExternalService = roleExternalService;
        this.organizationExternalService = organizationExternalService;
    }

    public Boolean validateUser(UUID userId) {
        return userRepository.existsById(userId);
    }

    public PageResponseDTO<UserDtoResponse> searchUsers(String search, int page, int size) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User authenticatedUser = (User) authentication.getPrincipal();
        UUID authenticatedUserOrganizationId = authenticatedUser.getOrganizationId();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> result = userRepository.searchUsers(search, authenticatedUserOrganizationId, pageable);
        Page<UserDtoResponse> dtoPage = result.map(userMapper::toDto);
        return new PageResponseDTO<>(dtoPage);
    }

    public PageResponseDTO<UserDtoResponse> searchUsersByOrganizationId(String search, int page, int size, UUID organizationId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> result = userRepository.searchUsers(search, organizationId, pageable);
        Page<UserDtoResponse> dtoPage = result.map(userMapper::toDto);
        return new PageResponseDTO<>(dtoPage);
    }

    public void assignRoleToUser(UUID userId, String roleName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User authenticatedUser = (User) authentication.getPrincipal();
        UUID authenticatedUserOrganizationId = authenticatedUser.getOrganizationId();

        String token = null;
        if (authentication != null) {
            token = (String) authentication.getCredentials();
        }

        if (!roleExternalService.roleExists(roleName, authenticatedUserOrganizationId, token )) {
            throw new BaseException(ConstantUtil.ROLE_NOT_FOUND, HttpStatus.BAD_REQUEST.value());
        }

        User user = userRepository.findByIdAndOrganizationId(userId, authenticatedUserOrganizationId)
                .orElseThrow(() -> new BaseException(ConstantUtil.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND.value()));

        user.setRole(roleName);
        userRepository.save(user);
    }

    public UserDtoResponse getUserById(UUID id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User authenticatedUser = (User) authentication.getPrincipal();
        UUID authenticatedUserOrganizationId = authenticatedUser.getOrganizationId();

        User user = userRepository.findByIdAndOrganizationId(id, authenticatedUserOrganizationId)
                .orElseThrow(() -> new BaseException(ConstantUtil.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND.value()));

        return userMapper.toDto(user);
    }

    public List<UserDtoResponse> getUsersByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<User> users = userRepository.findAllById(ids);
        return users.stream().map(userMapper::toDto).toList();
    }

    public UserDtoResponse addUser(UserDTO userDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();

        if (principal instanceof User authenticatedUser) {
            UUID organizationId = authenticatedUser.getOrganizationId();

            if (userRepository.existsByEmail(userDTO.getEmail())) {
                throw new BaseException(ConstantUtil.DUPLICATE_EMAIL, HttpStatus.BAD_REQUEST.value());
            }

            userDTO.setOrganizationId(organizationId);
            User savedUser = userRepository.save(userMapper.userDTOToUser(userDTO));
            return userMapper.toDto(savedUser);
        }
        else {
            throw new BaseException(ConstantUtil.PERMISSION_DENIED, HttpStatus.UNAUTHORIZED.value());
        }
    }

    public UserDtoResponse addUserWithOrganization(UserDTO userDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String token = null;
        if (authentication != null) {
            token = (String) authentication.getCredentials();
        }

        if (!organizationExternalService.organizationExists(userDTO.getOrganizationId(), token )) {
            throw new BaseException(ConstantUtil.ORGANIZATION_NOT_FOUND, HttpStatus.BAD_REQUEST.value());
        }

        if (userRepository.existsByEmail(userDTO.getEmail())) {
            throw new BaseException(ConstantUtil.DUPLICATE_EMAIL, HttpStatus.BAD_REQUEST.value());
        }

        User savedUser = userRepository.save(userMapper.userDTOToUser(userDTO));
        return userMapper.toDto(savedUser);
    }

    public void importUsers(MultipartFile file) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        UUID organizationId = null;

        if (principal instanceof User authenticatedUser) {
            organizationId = authenticatedUser.getOrganizationId();
        }

        List<User> users = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell emailCell = row.getCell(0);
                if (emailCell == null || emailCell.getCellType() != CellType.STRING) continue;

                String email = emailCell.getStringCellValue().trim();

                if (!userRepository.existsByEmail(email)) {
                    User user = new User();
                    user.setEmail(email);
                    user.setFirstName(null);
                    user.setLastName(null);
                    user.setGoogleId(null);
                    user.setPicture(null);
                    user.setRole(null);
                    user.setOrganizationId(organizationId);
                    users.add(user);
                }
            }
        } catch (Exception e) {
            throw new BaseException(ConstantUtil.ERROR_PROCESSING_FILE, HttpStatus.BAD_REQUEST.value());
        }

        userRepository.saveAll(users);
    }
}
