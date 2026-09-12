package com.example.firstapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
    name = "member_experiences",
    indexes = {
        @Index(
            name = "idx_experience_member",
            columnList = "member_id"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberExperience {

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
        name = "company_name",
        nullable = false,
        length = 150
    )
    private String companyName;

    @Column(
        name = "job_title",
        nullable = false,
        length = 150
    )
    private String jobTitle;

    @Column(
        name = "start_date",
        nullable = false
    )
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(
        name = "is_current_role",
        nullable = false
    )
    @Builder.Default
    private boolean currentRole = false;

    @Column(
        name = "description",
        columnDefinition = "TEXT"
    )
    private String description;
}