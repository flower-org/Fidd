package com.fidd.data.model;

import jakarta.persistence.*;

@Entity
@Table(name = "message_size")
public class MessageSize {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Column(name = "size")
    private Long size;

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

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
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
