package com.example.firstapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
    name = "member_educations",
    indexes = {
        @Index(
            name = "idx_education_member",
            columnList = "member_id"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberEducation {

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
        name = "institution_name",
        nullable = false,
        length = 150
    )
    private String institutionName;

    @Column(
        name = "degree",
        nullable = false,
        length = 150
    )
    private String degree;

    @Column(
        name = "field_of_study",
        length = 150
    )
    private String fieldOfStudy;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(
        name = "description",
        columnDefinition = "TEXT"
    )
    private String description;
}