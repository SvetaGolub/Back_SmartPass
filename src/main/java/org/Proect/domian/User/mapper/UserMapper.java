package org.Proect.domian.User.mapper;

import org.Proect.domian.User.dto.UserResponse;
import org.Proect.domian.User.model.User;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        if (user == null) return null;

        UserResponse dto = new UserResponse();
        dto.setId(user.getId());
        dto.setRoleName(user.getRole() != null ? user.getRole().getName() : "Без роли");
        dto.setZoneId(user.getZoneId());
        dto.setFullName(Stream.of(user.getLastName(), user.getFirstName(), user.getMiddleName())
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" ")));
        dto.setNumberPhone(user.getNumberPhone());
        dto.setEmail(user.getEmail());
        dto.setPosition(user.getPosition());
        dto.setPhotoUrl(user.getPhotoUrl());
        dto.setStatus(user.getStatus());
        dto.setBlockingReason(user.getBlockingReason());
        return dto;
    }
}