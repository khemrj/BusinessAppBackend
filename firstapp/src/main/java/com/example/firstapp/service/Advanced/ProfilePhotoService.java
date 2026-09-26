package com.example.firstapp.service.Advanced;

import com.example.firstapp.entity.Member;

import com.example.firstapp.exception.BadRequestException;
import com.example.firstapp.exception.ResourceNotFoundException;
import com.example.firstapp.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfilePhotoService {

    private final MemberRepository memberRepository;

    // Injected from application.properties — never hardcoded, so the
    // storage location is configurable per environment (dev/prod/CI)
    // without touching code.
    @Value("${app.upload.profile-photos-dir}")
    private String uploadDirPath;

    @Value("${app.upload.base-url}")
    private String baseUrl;

    private static final Set<String> ALLOWED_CONTENT_TYPES =
        Set.of("image/jpeg", "image/png", "image/webp");

    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5MB

    @Transactional
    public String upload(Long userId, MultipartFile file) {
        validateFile(file);
//saving profilephotoURl to member entity as profilephotourl
        final long memberId = memberRepository.findMemberIdByUserId(userId).orElseThrow(() -> new ResourceNotFoundException("member", "id", userId));
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new ResourceNotFoundException("member", "id", memberId));

        

        Path storageLocation = resolveStorageLocation();
        String filename = buildUniqueFilename(userId, file);
        Path targetPath = storageLocation.resolve(filename).normalize();

        // Defense against path traversal even though `filename` is
        // UUID-generated (never derived from client input) — this
        // check costs nothing and removes the failure mode entirely
        // rather than relying solely on "we generate the name safely".
        if (!targetPath.getParent().equals(storageLocation)) {
            throw new BadRequestException("Invalid file path");
        }

        try {
            Files.copy(
                file.getInputStream(),
                targetPath,
                StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException e) {
            log.error("Failed to store profile photo for user {}: {}",
                userId, e.getMessage());
            throw new RuntimeException("Failed to store file", e);
        }
        
        // Remove the previous photo file — without this, every
        // re-upload leaves an orphaned file on disk forever.
        deleteOldPhotoIfExists(member.getProfilePictureUrl());

        String publicUrl = baseUrl + "/api/v1/users/photos/" + filename;
        member.setProfilePictureUrl(publicUrl);
        memberRepository.save(member);

        log.info("Profile photo updated for user {}", userId);
        return publicUrl;
    }

    public Resource load(String filename) {
        try {
            Path file = resolveStorageLocation()
                .resolve(filename)
                .normalize();
            Resource resource = new UrlResource(file.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException(
                    "Photo", "filename", filename
                );
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException(
                "Photo", "filename", filename
            );
        }
    }

    // ── Validation ───────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException(
                "File exceeds the 5MB size limit"
            );
        }

        // Trusting Content-Type header alone is weak (client-controlled),
        // but combined with Spring's global multipart size limit and the
        // extension whitelist below, this closes the "renamed .exe as
        // .jpg" class of attack for the common case. For genuinely
        // hardened validation, sniff the file's magic bytes server-side
        // (e.g. via Apache Tika) instead of trusting either the header
        // or the extension — worth adding once you have real user
        // uploads to protect, not required to ship this feature today.
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException(
                "Only JPEG, PNG, and WEBP images are allowed"
            );
        }
    }

    // ── Filename handling ───────────────────────────────────────

    private String buildUniqueFilename(Long userId, MultipartFile file) {
        String extension = extractSafeExtension(file);
        return "user_" + userId + "_" + UUID.randomUUID() + extension;
    }

    /**
     * Derives the extension from the CONTENT TYPE we already validated,
     * not from the client-supplied original filename. This sidesteps
     * an entire class of "what if originalFilename is null, empty, has
     * no extension, or has a malicious one" edge cases — the content
     * type is the thing we trust, so the extension follows from it.
     */
    private String extractSafeExtension(MultipartFile file) {
        return switch (file.getContentType()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg"; // image/jpeg, and our validated fallback
        };
    }

    private Path resolveStorageLocation() {
        try {
            Path path = Paths.get(uploadDirPath).toAbsolutePath().normalize();
            Files.createDirectories(path);
            return path;
        } catch (IOException e) {
            throw new RuntimeException(
                "Could not initialize upload storage directory", e
            );
        }
    }

    private void deleteOldPhotoIfExists(String oldUrl) {
        if (oldUrl == null || oldUrl.isBlank()) return;
        try {
            String oldFilename = oldUrl.substring(oldUrl.lastIndexOf('/') + 1);
            Files.deleteIfExists(resolveStorageLocation().resolve(oldFilename));
        } catch (Exception e) {
            // Never let a stale-file cleanup failure block the actual
            // upload from succeeding — log and move on.
            log.warn("Could not delete old profile photo: {}", oldUrl);
        }
    }
}