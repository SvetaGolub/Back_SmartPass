package org.Proect.domian.User.service;

import lombok.RequiredArgsConstructor;
import org.Proect.domian.Enums.UserStatus;
import org.Proect.domian.User.dto.UserCreateRequest;
import org.Proect.domian.User.dto.UserResponse;
import org.Proect.domian.User.exception.RoleNotFoundException;
import org.Proect.domian.User.exception.UserAlreadyExistsException;
import org.Proect.domian.User.exception.UserNotFoundException;
import org.Proect.domian.User.mapper.UserMapper;
import org.Proect.domian.User.model.Roles;
import org.Proect.domian.User.model.User;
import org.Proect.domian.User.repository.RolesRepository;
import org.Proect.domian.User.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RolesRepository rolesRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponse getUserById(Integer id) {
        return userRepository.findById(id)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден с id: " + id));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(String name, Pageable pageable) {
        String term = escapeLike(name.trim().replaceAll("\\s+", " "));
        return userRepository.searchByFullName(term, pageable).map(userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByZone(Integer zoneId) {
        return userRepository.findByZoneId(zoneId).stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Transactional
    public UserResponse registerUser(UserCreateRequest request) {
        if (userRepository.existsByLogin(request.getLogin())) {
            throw new UserAlreadyExistsException("Логин уже занят");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email уже зарегистрирован");
        }
        if (userRepository.existsByNumberPhone(request.getNumberPhone())) {
            throw new UserAlreadyExistsException("Телефон уже используется");
        }

        Roles role = rolesRepository.findById(request.getRoleId())
                .orElseThrow(() -> new RoleNotFoundException("Роль не найдена с id: " + request.getRoleId()));

        User user = User.builder()
                .role(role)
                .zoneId(request.getZoneId())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .middleName(request.getMiddleName())
                .numberPhone(request.getNumberPhone())
                .email(request.getEmail())
                .position(request.getPosition())
                .address(request.getAddress())
                .birthday(request.getBirthday())
                .stage(request.getStage())
                .login(request.getLogin())
                .passwordHash(passwordEncoder.encode(request.getPassword())) // BCrypt-хеш
                .status(UserStatus.ACTIVE)
                .build();

        // saveAndFlush: нарушение unique-констрейнта (гонка) проявится здесь, а не при коммите,
        // и будет превращено в 409 обработчиком DataIntegrityViolationException
        User saved = userRepository.saveAndFlush(user);
        return userMapper.toResponse(saved);
    }

    @Transactional
    public void blockUser(Integer userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с id " + userId + " не найден"));

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new IllegalStateException("Пользователь уже заблокирован");
        }
        user.setStatus(UserStatus.BLOCKED);
        user.setBlockingReason(reason);
        // TODO: когда появится сущность токена QR-кода, деактивировать здесь текущий токен
    }

    @Transactional
    public void unblockUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с id " + userId + " не найден"));

        user.setStatus(UserStatus.ACTIVE);
        user.setBlockingReason(null);
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}