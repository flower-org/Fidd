package com.fidd.data.model;

import jakarta.persistence.*;

@Entity
@Table(name = "signatures")
public class Signature {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private SignatureType type;

    @Lob
    @Column(name = "signature")
    private byte[] signature;

    @Column(name = "last_access_time")
    private Long lastAccessTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public SignatureType getType() {
        return type;
    }

    public void setType(SignatureType type) {
        this.type = type;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    @PrePersist
    @PreUpdate
    public void updateLastAccessTime() {
        this.lastAccessTime = System.currentTimeMillis();
    }

    public Long getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(Long lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }
}
