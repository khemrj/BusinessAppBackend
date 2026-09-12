package com.example.firstapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
    name = "member_certifications",
    indexes = {
        @Index(
            name = "idx_certification_member",
            columnList = "member_id"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberCertification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "member_id",
        nullable = false
    )
    private Member member;

    @Column(
        name = "title",
        nullable = false,
        length = 150
    )
    private String title;

    @Column(
        name = "issuing_organization",
        nullable = false,
        length = 150
    )
    private String issuingOrganization;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(
        name = "credential_id",
        length = 150
    )
    private String credentialId;

    @Column(
        name = "credential_url",
        length = 500
    )
    private String credentialUrl;
}