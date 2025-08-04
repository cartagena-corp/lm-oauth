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

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    public Boolean validateUser(UUID userId) {
        return userRepository.existsById(userId);
    }

    public PageResponseDTO<UserDtoResponse> searchUsers(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> result = userRepository.searchUsers(search, pageable);
        Page<UserDtoResponse> dtoPage = result.map(userMapper::toDto);
        return new PageResponseDTO<>(dtoPage);
    }

    public void assignRoleToUser(UUID userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(ConstantUtil.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND.value()));
        user.setRole(roleName);
        userRepository.save(user);
    }

    public UserDtoResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
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

    public void addUser(UserDTO userDTO) {
        if (userRepository.existsByEmail(userDTO.getEmail())) {
            throw new BaseException(ConstantUtil.DUPLICATE_EMAIL, HttpStatus.BAD_REQUEST.value());
        }
        userRepository.save(userMapper.userDTOToUser(userDTO));
    }

    public void importUsers(MultipartFile file) {
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
                    users.add(user);
                }
            }
        } catch (Exception e) {
            throw new BaseException(ConstantUtil.ERROR_PROCESSING_FILE, HttpStatus.BAD_REQUEST.value());
        }

        userRepository.saveAll(users);
    }
}
