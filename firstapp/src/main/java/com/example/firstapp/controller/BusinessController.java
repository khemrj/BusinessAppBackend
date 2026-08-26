package com.example.firstapp.controller;

import com.example.firstapp.dto.BusinessRegistrationRequest;
import com.example.firstapp.dto.BusinessResponse;
import com.example.firstapp.dto.BusinessUpdateRequest;
import com.example.firstapp.entity.Business;
import com.example.firstapp.service.BusinessService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/businesses")
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessService businessService;

    /**
     * Public — no auth required, this IS how a business gets an account.
     * Creates the User (ROLE_BUSINESS) + Business row together.
     */
    @PostMapping("/register")
    public ResponseEntity<BusinessResponse> register(@RequestBody @Valid BusinessRegistrationRequest req) {
        Business business = businessService.registerBusiness(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(BusinessResponse.fromEntity(business));
    }

    /** The authenticated business viewing its own profile. */
    @GetMapping("/me")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<BusinessResponse> getMyProfile(Authentication authentication) {
        Long userId = currentUserId(authentication);
        Business business = businessService.getByOwnerUserId(userId);
        return ResponseEntity.ok(BusinessResponse.fromEntity(business));
    }

    /** The authenticated business editing its own profile (partial update). */
    @PatchMapping("/me")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<BusinessResponse> updateMyProfile(Authentication authentication,
                                                              @RequestBody @Valid BusinessUpdateRequest req) {
        Long userId = currentUserId(authentication);
        Business business = businessService.updateOwnProfile(userId, req);
        return ResponseEntity.ok(BusinessResponse.fromEntity(business));
    }

    /** Public business profile view — e.g. tapping a company name from an opportunity listing. */
    @GetMapping("/{id}")
    public ResponseEntity<BusinessResponse> getById(@PathVariable Long id) {
        Business business = businessService.getById(id);
        return ResponseEntity.ok(BusinessResponse.fromEntity(business));
    }

    /** Admin-only KYB decision. */
    @PatchMapping("/{id}/verification")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BusinessResponse> decideVerification(@PathVariable Long id,
                                                                 @RequestParam boolean approve,
                                                                 Authentication authentication) {
        Long adminId = currentUserId(authentication);
        Business business = businessService.verify(id, adminId, approve);
        return ResponseEntity.ok(BusinessResponse.fromEntity(business));
    }

    /**
     * TODO — adjust to match however your JWT filter actually populates the principal.
     * This is a placeholder; see note below.
     */
    private Long currentUserId(Authentication authentication) {
        // e.g. ((YourUserPrincipalClass) authentication.getPrincipal()).getId();
        throw new UnsupportedOperationException("wire this up to your actual principal type");
    }
}
