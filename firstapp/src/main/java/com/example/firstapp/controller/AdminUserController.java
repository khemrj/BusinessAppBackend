package com.example.firstapp.controller;

import com.example.firstapp.dto.ChangeRoleRequest;
import com.example.firstapp.dto.UpdateStatusRequest;
import com.example.firstapp.dto.UserSummaryDto;
import com.example.firstapp.entity.User;
import com.example.firstapp.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")     // every method requires ADMIN
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    // Rule 2: whitelist sortable fields — these are ENTITY property names,
    // not DB column names (e.g. createdAt, not created_at)
    private static final Set<String> ALLOWED_SORT =
            Set.of("id", "email", "role", "createdAt", "lastLoginAt", "failedAttempts");

    private static final int MAX_PAGE_SIZE = 100;   // Rule 1

    /** Paginated, sortable list of users. */
    @GetMapping
    public Page<UserSummaryDto> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "true") boolean ascending) {

        // Rule 1: cap the size so nobody can request a million rows
        size = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        // Rule 1b: guard against negative page
        page = Math.max(page, 0);

        // Rule 2: reject unknown sort fields, fall back to a safe default
        if (!ALLOWED_SORT.contains(sortBy)) {
            sortBy = "id";
        }

        // Rule 3: always append a unique tiebreaker (id) so pages stay stable
        Sort sort = (ascending
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending())
                .and(Sort.by("id").ascending());

        Pageable pageable = PageRequest.of(page, size, sort);
        // Rule 7: service returns Page<UserSummaryDto> — no password hash
        return adminUserService.listUsers(pageable);
    }

    /** Single user detail. */
    @GetMapping("/{id}")
    public UserSummaryDto getUser(@PathVariable Long id) {
        return adminUserService.getUser(id);
    }

    /** Activate / deactivate an account. */
    @PatchMapping("/{id}/status")
    public UserSummaryDto setStatus(
            @PathVariable Long id,
            @RequestBody @Valid UpdateStatusRequest req,
            @AuthenticationPrincipal User actingAdmin) {
        return adminUserService.setActive(id, req.active(), actingAdmin.getId());
    }

    /** Change a user's role (promote / demote). */
    @PatchMapping("/{id}/role")
    public UserSummaryDto changeRole(
            @PathVariable Long id,
            @RequestBody @Valid ChangeRoleRequest req,
            @AuthenticationPrincipal User actingAdmin) {
        return adminUserService.changeRole(id, req.role(), actingAdmin.getId());
    }

    /** Unlock a locked account and reset failed attempts. */
    @PatchMapping("/{id}/unlock")
    public UserSummaryDto unlock(@PathVariable Long id) {
        return adminUserService.unlock(id);
    }
}