package com.example.firstapp.service;

import com.example.firstapp.dto.UserSummaryDto;
import com.example.firstapp.entity.User;
import com.example.firstapp.enums.Role;
import com.example.firstapp.exception.IllegalOperationException;
import com.example.firstapp.exception.UserNotFoundException;
import com.example.firstapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository users;

    @Transactional(readOnly = true)
    public Page<UserSummaryDto> listUsers(@NonNull Pageable pageable) {
        // .map preserves pagination metadata while swapping entity → DTO (Rule 7)
        return users.findAll(pageable).map(UserSummaryDto::from);
    }

    @Transactional(readOnly = true)
    public UserSummaryDto getUser(@NonNull Long id) {
        return users.findById(id)
                .map(UserSummaryDto::from)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional
    public UserSummaryDto setActive(Long id, boolean active, Long actingAdminId) {
        User user = users.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        // Guardrail: an admin must not deactivate themselves
        if (user.getId().equals(actingAdminId) && !active) {
            throw new IllegalOperationException("An admin cannot deactivate themselves");
        }

        user.setActive(active);   // managed entity → flushes on commit
        return UserSummaryDto.from(user);
    }

    @Transactional
    public UserSummaryDto changeRole(Long id, Role newRole, Long actingAdminId) {
        User user = users.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        // Guardrail: an admin must not demote themselves (lockout risk)
        if (user.getId().equals(actingAdminId) && newRole != Role.ROLE_ADMIN) {
            throw new IllegalOperationException("An admin cannot demote themselves");
        }

        user.setRole(newRole);
        return UserSummaryDto.from(user);
    }

    @Transactional
    public UserSummaryDto unlock(Long id) {
        User user = users.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        user.setLocked(false);
        user.setFailedAttempts(0);
        user.setLockTime(null);
        return UserSummaryDto.from(user);
    }
}