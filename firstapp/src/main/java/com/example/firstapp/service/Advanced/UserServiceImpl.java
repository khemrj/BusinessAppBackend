package com.example.firstapp.service.Advanced;

import com.example.firstapp.repository.MemberRepository;
import com.example.firstapp.repository.UserRepository;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UserService Implementation
 * 
 * Fetches chat profile details from the application's user and member repositories.
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final SimpUserRegistry userRegistry;

    public UserServiceImpl(
            UserRepository userRepository,
            MemberRepository memberRepository,
            SimpUserRegistry userRegistry) {
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
        this.userRegistry = userRegistry;
    }

    @Override
    public String getUserName(Long userId) {
        if (userId == null) {
            return "Unknown";
        }
        return userRepository.findById(userId)
                .map(user -> user.getUsername())
                .orElse("Unknown");
    }

    @Override
    public String getAvatarUrl(Long userId) {
        if (userId == null) {
            return null;
        }
        return memberRepository.findProfilePictureUrlByUserId(userId).orElse(null);
    }

    @Override
    public String getUserStatus(Long userId) {
        return userId != null && userRegistry.getUser(userId.toString()) != null
                ? "online"
                : "offline";
    }

    @Override
    public boolean userExists(Long userId) {
        return userId != null && userRepository.existsById(userId);
    }

    @Override
    public boolean canContact(Long userA, Long userB) {
        // No block-list or messaging-privacy repository exists yet; this only validates distinct, existing accounts.
        return userA != null
                && userB != null
                && !userA.equals(userB)
                && userRepository.existsById(userA)
                && userRepository.existsById(userB);
    }
}
