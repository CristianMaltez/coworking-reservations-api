package com.coworking.reservation.auth.dto;

import com.coworking.reservation.domain.entity.Role;
import com.coworking.reservation.domain.entity.User;
import com.coworking.reservation.domain.enums.UserStatus;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        UserStatus status,
        Set<String> roles
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getStatus(),
                user.getRoles().stream()
                        .map(Role::getCode)
                        .collect(Collectors.toUnmodifiableSet())
        );
    }
}
