package com.example.firstapp.entity.Messaging;



import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "chat_participants",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_participant",
        columnNames = {"conversation_id", "user_id"}
    ),
    indexes = @Index(name = "idx_participant_user", columnList = "user_id")
)
public class ConversationParticipant {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "conversation_id", nullable = false) private Long conversationId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_part_conv"))
    private Conversation conversation;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "last_read_message_id") private Long lastReadMessageId;
    @Column(name = "joined_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    private Instant joinedAt = Instant.now();

    protected ConversationParticipant() {}

    public ConversationParticipant(Long conversationId, Long userId) {
        this.conversationId = conversationId;
        this.userId = userId;
    }

    /** Read pointer only moves forward. */
    public void markRead(Long messageId) {
        if (lastReadMessageId == null || messageId > lastReadMessageId) {
            lastReadMessageId = messageId;
        }
    }

    public Long getConversationId() { return conversationId; }
    public Long getUserId() { return userId; }
    public Long getLastReadMessageId() { return lastReadMessageId; }
    public Instant getJoinedAt() { return joinedAt; }
}
