package com.example.firstapp.service;


import com.example.firstapp.dto.BusinessRegistrationRequest;
import com.example.firstapp.dto.BusinessUpdateRequest;
import com.example.firstapp.entity.Business;
import com.example.firstapp.entity.User;
import com.example.firstapp.enums.BusinessStatus;
import com.example.firstapp.enums.VerificationStatus;
import com.example.firstapp.exception.EmailAlreadyExistsException;
import com.example.firstapp.exception.ResourceNotFoundException;
import com.example.firstapp.repository.BusinessRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BusinessService {

  
    private final BusinessRepository businessRepository;
  

    @Transactional
    public Business registerBusiness(BusinessRegistrationRequest req, User user) {

       
       
        if (businessRepository.existsByContactEmail(req.contactEmail())) {
            throw new EmailAlreadyExistsException("Contact email is already in use");
        }

        // User user = User.builder()
        //         .email(req.email())
        //         .username(req.email())
        //         .password(passwordEncoder.encode(req.password()))
        //         .role(Role.ROLE_BUSINESS)
        //         .build();
        // userRepository.save(user);

        Business business = Business.builder()
            .owner(user)
                .companyName(req.companyName())
                .registrationNumber(req.registrationNumber())
                .industryType(req.industryType())
                .companySize(req.companySize())
                .website(req.website())
                .description(req.description())
              
                
                .verificationStatus(VerificationStatus.PENDING)
                .status(BusinessStatus.ACTIVE)
                .build();

        return businessRepository.save(business);
    }

    @Transactional(readOnly = true)
public Business getByOwnerUserId(Long userId) {
    return businessRepository.findByOwner_Id(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Business", "ownerUserId", userId));
}

@Transactional(readOnly = true)
public Business getById(Long businessId) {
    return businessRepository.findById(businessId)
            .orElseThrow(() -> new ResourceNotFoundException("Business", "id", businessId));
}
    

    @Transactional
    public Business updateOwnProfile(Long userId, BusinessUpdateRequest req) {
        Business business = getByOwnerUserId(userId);

        if (req.companyName() != null) business.setCompanyName(req.companyName());
        if (req.industryType() != null) business.setIndustryType(req.industryType());
        if (req.companySize() != null) business.setCompanySize(req.companySize());
        if (req.website() != null) business.setWebsite(req.website());
        if (req.description() != null) business.setDescription(req.description());
        if (req.logoUrl() != null) business.setLogoUrl(req.logoUrl());
        if (req.headquarters() != null) business.setHeadquarters(req.headquarters().toEntity());
        if (req.contactPhone() != null) business.setContactPhone(req.contactPhone());

        // dirty checking flushes on transaction commit — no explicit save() needed
        return business;
    }

    @Transactional
    public Business verify(Long businessId, Long adminUserId, boolean approve) {
        Business business = getById(businessId);
        business.setVerificationStatus(approve ? VerificationStatus.VERIFIED : VerificationStatus.REJECTED);
        business.setVerifiedAt(java.time.LocalDateTime.now());
        business.setVerifiedByAdminId(adminUserId);
        return business;
    }

    /**
     * Ownership check used by OpportunityService before letting a business
     * mutate an opportunity — confirms the JWT-authenticated user actually
     * owns the Business row they're acting as.
     */
    @Transactional(readOnly = true)
    public Long getBusinessIdForOwner(Long userId) {
        return getByOwnerUserId(userId).getId();
    }
}