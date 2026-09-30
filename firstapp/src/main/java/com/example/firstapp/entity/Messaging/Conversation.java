package com.example.firstapp.entity.Messaging;


import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "chat_conversations",
    uniqueConstraints = @UniqueConstraint(name = "uk_conv_direct_key", columnNames = "direct_key")
)
public class Conversation {
    public enum Type { DIRECT, GROUP }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private Type type;

    @Column(name = "direct_key", length = 64)
    private String directKey;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    private Instant createdAt = Instant.now();

    protected Conversation() {}

    public Conversation(Type type, String directKey) {
        this.type = type;
        this.directKey = directKey;
    }

    public Long getId() { return id; }
    public Type getType() { return type; }
    public Instant getCreatedAt() { return createdAt; }
    
    public String getDirectKey() { return directKey; }
}