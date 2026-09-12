package com.example.firstapp.service;
import java.text.Normalizer;

import org.springframework.stereotype.Service;

import com.example.firstapp.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfileSlugService {

    private final MemberRepository memberRepository;

    public String generate(String firstName, String lastName) {
        String base = Normalizer.normalize(firstName + "-" + lastName, Normalizer.Form.NFD)
                .replaceAll("[^a-zA-Z0-9\\s-]", "")
                .trim()
                .toLowerCase()
                .replaceAll("\\s+", "-");

        String candidate = base;
        int suffix = 1;

        // Loop guarantees uniqueness even with many "john-smith"s
        while (memberRepository.existsByProfileSlug(candidate)) {
            suffix++;
            candidate = base + "-" + suffix;
        }
        return candidate; // "john-smith", then "john-smith-2", "john-smith-3"...
    }
}