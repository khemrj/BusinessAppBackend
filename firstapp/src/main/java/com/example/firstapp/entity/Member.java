package com.example.firstapp.entity;

import com.example.firstapp.enums.FinanceSpecialization;
import com.example.firstapp.enums.ProfileVisibility;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(
    name = "members",
    indexes = {
        @Index(
            name = "idx_member_user_id",
            columnList = "user_id"
        ),
        @Index(
            name = "idx_member_profile_slug",
            columnList = "profile_slug"
        )
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_member_user_id",
            columnNames = "user_id"
        ),
        @UniqueConstraint(
            name = "uk_member_profile_slug",
            columnNames = "profile_slug"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

    // =========================================================
    // ID
    // =========================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // =========================================================
    // USER / AUTHENTICATION
    // =========================================================

    /**
     * Member owns a professional profile.
     * User owns authentication.
     */
    @OneToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "user_id",
        nullable = false,
        unique = true
    )
    private User user;


    // =========================================================
    // BASIC INFORMATION
    // =========================================================

    @Column(
        name = "first_name",
        nullable = false,
        length = 100
    )
    private String firstName;

    @Column(
        name = "last_name",
        nullable = false,
        length = 100
    )
    private String lastName;


    // =========================================================
    // PUBLIC PROFILE
    // =========================================================

    /**
     * LinkedIn-style URL identifier.
     *
     * Example:
     * /in/khem-raj-joshi
     */
    @Column(
        name = "profile_slug",
        nullable = false,
        unique = true,
        length = 100
    )
    private String profileSlug;

    /**
     * Example:
     * "Computer Engineer | Spring Boot | Flutter"
     */
    @Column(
        name = "headline",
        length = 255
    )
    private String headline;

    /**
     * LinkedIn-style About section.
     */
    @Column(
        name = "bio",
        columnDefinition = "TEXT"
    )
    private String bio;

    /**
     * Main profile image.
     *
     * Store URL/object-storage key, NOT image bytes.
     */
    @Column(
        name = "profile_picture_url",
        length = 500
    )
    private String profilePictureUrl;

    /**
     * LinkedIn-style cover/banner image.
     */
    @Column(
        name = "cover_photo_url",
        length = 500
    )
    private String coverPhotoUrl;


    // =========================================================
    // PROFESSIONAL INFORMATION
    // =========================================================

@Enumerated(EnumType.STRING)
@Column(name = "specialization", length = 40)
private FinanceSpecialization specialization;

    /**
     * Public professional contact email.
     *
     * Different from User.email.
     *
     * Nullable because not every member wants to expose one.
     */
    @Column(
        name = "contact_email",
        length = 254
    )
    private String contactEmail;

    @Column(
        name = "phone",
        length = 30
    )
    private String phone;

    @Column(
        name = "years_of_experience"
    )
    private Integer yearsOfExperience;

    @Builder.Default
    @Column(
        name = "open_to_work",
        nullable = false
    )
    private boolean openToWork = false;


    // =========================================================
    // SOCIAL / PROFESSIONAL LINKS
    // =========================================================

    @Column(
        name = "website_url",
        length = 500
    )
    private String websiteUrl;

    @Column(
        name = "linkedin_url",
        length = 500
    )
    private String linkedinUrl;

    @Column(
        name = "github_url",
        length = 500
    )
    private String githubUrl;

    @Column(
        name = "resume_url",
        length = 500
    )
    private String resumeUrl;


    // =========================================================
    // PROFILE VISIBILITY
    // =========================================================

    @Enumerated(EnumType.STRING)
    @Column(
        name = "profile_visibility",
        nullable = false,
        length = 30
    )
    @Builder.Default
    private ProfileVisibility profileVisibility =
        ProfileVisibility.PUBLIC;


    // =========================================================
    // ADDRESS
    // =========================================================

    @Embedded
    private Address location;


    // =========================================================
    // SKILLS
    // =========================================================

    /**
     * Skills are value data, not independent entities.
     *
     * Set is preferable because:
     *
     * Java
     * Java
     * Java
     *
     * should not be three separate skills.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "member_skills",
        joinColumns = @JoinColumn(
            name = "member_id",
            nullable = false
        ),
        uniqueConstraints = {
            @UniqueConstraint(
                name = "uk_member_skill",
                columnNames = {
                    "member_id",
                    "skill"
                }
            )
        }
    )
    @Column(
        name = "skill",
        nullable = false,
        length = 100
    )
    @BatchSize(size = 25)
    @Builder.Default
    private Set<String> skills = new HashSet<>();


    // =========================================================
    // EXPERIENCE
    // =========================================================

    /**
     * Intentionally kept as List.
     *
     * DO NOT solve MultipleBagFetchException by blindly
     * converting every relationship into Set.
     *
     * We will fetch this collection explicitly when needed.
     */
    @OneToMany(
        mappedBy = "member",
        fetch = FetchType.LAZY,
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @BatchSize(size = 25)
    @Builder.Default
    private List<MemberExperience> experiences =
        new ArrayList<>();


    // =========================================================
    // EDUCATION
    // =========================================================

    @OneToMany(
        mappedBy = "member",
        fetch = FetchType.LAZY,
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @BatchSize(size = 25)
    @Builder.Default
    private List<MemberEducation> educations =
        new ArrayList<>();


    // =========================================================
    // CERTIFICATIONS
    // =========================================================

    @OneToMany(
        mappedBy = "member",
        fetch = FetchType.LAZY,
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @BatchSize(size = 25)
    @Builder.Default
    private List<MemberCertification> certifications =
        new ArrayList<>();


    // =========================================================
    // OPTIMISTIC LOCKING
    // =========================================================

    /**
     * Prevents silent lost updates when two requests update
     * the same profile concurrently.
     */
    @Version
    @Column(
        name = "version",
        nullable = false
    )
    private Long version;


    // =========================================================
    // AUDIT
    // =========================================================

    @CreationTimestamp
    @Column(
        name = "created_at",
        nullable = false,
        updatable = false
    )
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(
        name = "updated_at",
        nullable = false
    )
    private LocalDateTime updatedAt;


    // =========================================================
    // HELPER METHODS
    // =========================================================

    public void addSkill(String skill) {

        if (skill == null || skill.isBlank()) {
            return;
        }

        skills.add(skill.trim());
    }

    public void removeSkill(String skill) {

        if (skill == null) {
            return;
        }

        skills.remove(skill.trim());
    }

    public void addExperience(MemberExperience experience) {

        if (experience == null) {
            return;
        }

        experiences.add(experience);
        experience.setMember(this);
    }

    public void removeExperience(MemberExperience experience) {

        if (experience == null) {
            return;
        }

        experiences.remove(experience);
        experience.setMember(null);
    }

    public void addEducation(MemberEducation education) {

        if (education == null) {
            return;
        }

        educations.add(education);
        education.setMember(this);
    }

    public void removeEducation(MemberEducation education) {

        if (education == null) {
            return;
        }

        educations.remove(education);
        education.setMember(null);
    }

    public void addCertification(
        MemberCertification certification
    ) {

        if (certification == null) {
            return;
        }

        certifications.add(certification);
        certification.setMember(this);
    }

    public void removeCertification(
        MemberCertification certification
    ) {

        if (certification == null) {
            return;
        }

        certifications.remove(certification);
        certification.setMember(null);
    }

    public String getFullName() {

        return firstName + " " + lastName;
    }
}
