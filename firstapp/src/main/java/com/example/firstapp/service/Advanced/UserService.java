package com.example.firstapp.service.Advanced;



/**
 * UserService
 * 
 * Interface for fetching user details needed by the chat system.
 * Implement this with your actual User/Profile entity and repository.
 * 
 * This is injected into ChatService to denormalize user data in message DTOs.
 */
public interface UserService {

    /**
     * Get a user's display name.
     * 
     * @param userId the user ID
     * @return the user's display name, or "Unknown" if not found
     */
    String getUserName(Long userId);

    /**
     * Get a user's avatar URL.
     * 
     * @param userId the user ID
     * @return the user's avatar URL, or null if not set
     */
    String getAvatarUrl(Long userId);

    /**
     * Get a user's online status.
     * 
     * @param userId the user ID
     * @return one of: "online", "offline", "away" (or custom status)
     */
    String getUserStatus(Long userId);

    /**
     * Verify a user exists.
     * Called when starting a direct conversation to ensure both users exist.
     * 
     * @param userId the user ID
     * @return true if user exists
     */
    boolean userExists(Long userId);

    /**
     * Check if userA can contact userB.
     * Implement your block list / privacy rules here.
     * 
     * @param userA the sender
     * @param userB the recipient
     * @return true if userA can message userB
     */
    boolean canContact(Long userA, Long userB);
}
