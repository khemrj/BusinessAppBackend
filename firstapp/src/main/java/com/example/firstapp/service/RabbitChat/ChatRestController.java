package com.example.firstapp.service.RabbitChat;

import com.example.firstapp.dto.Chat.ChatDtos.*;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController("rabbitChatRestController")
@RequestMapping("/api/chat")
public class ChatRestController {

    private final ChatPersistenceService chat;

    public ChatRestController(ChatPersistenceService chat) { this.chat = chat; }

    /** Call on every (re)connect and app resume: GET /sync?afterId=<highest id you hold> */
    @GetMapping("/conversations/{id}/sync")
    public SyncResponse sync(@PathVariable Long id,
                             @RequestParam(defaultValue = "0") long afterId,
                             @RequestParam(defaultValue = "50") int limit,
                             Principal principal) {
        return chat.syncAfter(uid(principal), id, afterId, clamp(limit));
    }

    /** Scroll-back: GET /history?beforeId=<oldest id you hold> */
    @GetMapping("/conversations/{id}/history")
    public HistoryResponse history(@PathVariable Long id,
                                   @RequestParam(required = false) Long beforeId,
                                   @RequestParam(defaultValue = "50") int limit,
                                   Principal principal) {
        return chat.history(uid(principal), id, beforeId, clamp(limit));
    }

    @PostMapping("/read")
    public MarkReadAckDto markRead(@Valid @RequestBody BulkMarkReadRequest req, Principal principal) {
        return chat.markRead(uid(principal), req.conversationId(), req.messageId());
    }

    private static int clamp(int limit) { return Math.max(1, Math.min(limit, 100)); }

    // ADAPT: must return the same userId your JWT carries (the STOMP interceptor uses the "userId" claim).
    private static Long uid(Principal p) { return Long.valueOf(p.getName()); }
}
