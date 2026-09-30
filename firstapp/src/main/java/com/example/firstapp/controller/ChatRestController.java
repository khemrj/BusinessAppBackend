package com.example.firstapp.controller;





import com.example.firstapp.dto.Chat.ChatDtos.*;
import com.example.firstapp.service.Advanced.ChatService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/api/chat")
public class ChatRestController {

    private final ChatService chatService;

    public ChatRestController(ChatService chatService) {
        this.chatService = chatService;
    }

    private static Long uid(Principal p) {
        return Long.valueOf(p.getName());
    }

    // ========== CONVERSATIONS ==========

    /**
     * Start or get a direct (1:1) conversation.
     */
    @PostMapping("/conversations/direct")
    @ResponseStatus(HttpStatus.CREATED)
    public StartDirectResponse startDirect(
            @Valid @RequestBody StartDirectRequest req,
            Principal p) {
        return chatService.getOrCreateDirect(uid(p), req.userId());
    }

    /**
     * List all conversations for the current user.
     * Sorted by most recent activity first.
     */
    @GetMapping("/conversations")
    public ConversationListResponse listConversations(Principal p) {
        return chatService.listConversations(uid(p));
    }

    /**
     * Get full conversation details with participant info.
     */
    @GetMapping("/conversations/{id}")
    public ConversationDetailDto getConversationDetail(
            @PathVariable Long id,
            Principal p) {
        return chatService.getConversationDetail(uid(p), id);
    }

    // ========== MESSAGES: HISTORY & SYNC ==========

    /**
     * Get message history (newest first, paginated backwards).
     * Pass the smallest ID from last response as ?before= for next page.
     */
    @GetMapping("/conversations/{id}/messages")
    public HistoryResponse history(
            @PathVariable Long id,
            @RequestParam(required = false) Long before,
            @RequestParam(defaultValue = "50") int limit,
            Principal p) {
        return chatService.history(uid(p), id, before, limit);
    }

    /**
     * Sync messages after reconnect (oldest first).
     * Pass the last message ID you saw as ?after= to backfill missed messages.
     * Useful when resuming the app or reconnecting WebSocket.
     */
    @GetMapping("/conversations/{id}/messages/since")
    public SyncResponse sync(
            @PathVariable Long id,
            @RequestParam Long after,
            @RequestParam(defaultValue = "200") int limit,
            Principal p) {
        return chatService.sync(uid(p), id, after, limit);
    }

    // ========== READ STATE ==========

    /**
     * Mark a single message as read.
     */
    @PostMapping("/conversations/{id}/read")
    public MarkReadAckDto markRead(
            @PathVariable Long id,
            @Valid @RequestBody MarkReadRequest req,
            Principal p) {
        return chatService.markRead(uid(p), id, req.messageId());
    }

    /**
     * Mark all messages up to messageId as read (bulk operation).
     * More efficient than sending individual mark-read requests.
     */
    @PostMapping("/conversations/{id}/read-bulk")
    public MarkReadAckDto markReadBulk(
            @PathVariable Long id,
            @Valid @RequestBody BulkMarkReadRequest req,
            Principal p) {
        return chatService.markReadBulk(uid(p), id, req.messageId());
    }

    /**
     * Get unread counts for all conversations.
     * Useful for rendering conversation list badges.
     */
    @GetMapping("/unread-counts")
    public UnreadCountsResponse getUnreadCounts(Principal p) {
        return chatService.getUnreadCounts(uid(p));
    }
}
