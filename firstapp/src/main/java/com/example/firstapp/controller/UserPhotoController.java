package com.example.firstapp.controller;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.firstapp.service.Advanced.ProfilePhotoService;

import java.io.IOException;
import java.nio.file.Files;

@RestController
@RequestMapping("/api/v1/users")
public class UserPhotoController {

    private final ProfilePhotoService profilePhotoService; // Replace with your actual service class name

    public UserPhotoController(ProfilePhotoService profilePhotoService) {
        this.profilePhotoService = profilePhotoService;
    }

    @GetMapping("/photos/{filename:.+}")
    public ResponseEntity<Resource> serveProfilePhoto(@PathVariable String filename) {
        // 1. Calls your service load method (which throws ResourceNotFoundException if missing)
        Resource resource = profilePhotoService.load(filename);

        // 2. Automatically detect content type (e.g., image/jpeg, image/png)
        String contentType = "application/octet-stream";
        try {
            contentType = Files.probeContentType(resource.getFile().toPath());
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
        } catch (IOException e) {
            // Fallback content type if probing fails
        }

        // 3. Return the resource with headers configured for inline viewing
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}