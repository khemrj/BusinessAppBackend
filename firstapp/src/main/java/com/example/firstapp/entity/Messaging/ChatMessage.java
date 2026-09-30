package com.example.firstapp.entity.Messaging;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "chat_messages",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_sender_client_msg",
        columnNames = {"sender_id", "client_msg_id"}
    ),
    indexes = @Index(name = "idx_msg_conv_id", columnList = "conversation_id, id")
)
public class ChatMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "conversation_id", nullable = false) private Long conversationId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_msg_conv"))
    private Conversation conversation;
    @Column(name = "sender_id", nullable = false) private Long senderId;
    @Column(name = "client_msg_id", nullable = false, length = 36) private String clientMsgId;
    @Column(nullable = false, length = 4000) private String content;
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    private Instant createdAt = Instant.now();

    protected ChatMessage() {}

    public ChatMessage(Long conversationId, Long senderId, String clientMsgId, String content) {
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.clientMsgId = clientMsgId;
        this.content = content;
    }

    public Long getId() { return id; }
    public Long getConversationId() { return conversationId; }
    public Long getSenderId() { return senderId; }
    public String getClientMsgId() { return clientMsgId; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
}