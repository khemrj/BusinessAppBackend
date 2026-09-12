package com.example.firstapp.dto.Member;

import com.example.firstapp.entity.Address;
import com.example.firstapp.entity.Member;

public record MemberProfileResponse(

        Long id,

        String firstName,

        String lastName,

        String profileSlug,

        String headline,

        String bio,

        String profilePictureUrl,

        String coverPhotoUrl,

        boolean openToWork,

        String websiteUrl,

        String linkedinUrl,

        String githubUrl,

        String resumeUrl,

        Address location

) {

    public static MemberProfileResponse fromEntity(Member member) {

        if (member == null) {
            return null;
        }

        return new MemberProfileResponse(
                member.getId(),
                member.getFirstName(),
                member.getLastName(),
                member.getProfileSlug(),
                member.getHeadline(),
                member.getBio(),
                member.getProfilePictureUrl(),
                member.getCoverPhotoUrl(),
                member.isOpenToWork(),
                member.getWebsiteUrl(),
                member.getLinkedinUrl(),
                member.getGithubUrl(),
                member.getResumeUrl(),
                member.getLocation()
        );
    }
}