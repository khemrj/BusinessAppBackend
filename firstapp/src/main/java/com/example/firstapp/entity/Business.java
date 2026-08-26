
package com.example.firstapp.entity;
import jakarta.persistence.Entity;

import com.example.firstapp.enums.IndustryType;

import com.example.firstapp.enums.VerificationStatus;
import com.example.firstapp.dto.AddressRequest;
import com.example.firstapp.enums.BusinessStatus;
import com.example.firstapp.enums.CompanySize;

import jakarta.persistence.CascadeType;

import jakarta.persistence.Column;

import jakarta.persistence.Embedded;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import jakarta.persistence.JoinColumn;

import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import lombok.NoArgsConstructor;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "businesses")
@Data
@NoArgsConstructor @AllArgsConstructor @Builder
public class Business {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User owner;                     // the ROLE_BUSINESS user managing this profile

    @Column(nullable = false)
    private String companyName;

    @Column(unique = true, nullable = false)
    private String registrationNumber;

    @Enumerated(EnumType.STRING)
    private IndustryType industryType;

    @Enumerated(EnumType.STRING)
    private CompanySize companySize;

    private String website;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String logoUrl;

    @Embedded
    private Address headquarters;

    @Column(nullable = false, unique = true)
    private String contactEmail;

    private String contactPhone;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.PENDING; // PENDING, VERIFIED, REJECTED

    private LocalDateTime verifiedAt;
    private Long verifiedByAdminId;         // audit trail — which admin approved/rejected

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private BusinessStatus status = BusinessStatus.ACTIVE;

    @OneToMany(mappedBy = "business", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Opportunity> opportunities = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
