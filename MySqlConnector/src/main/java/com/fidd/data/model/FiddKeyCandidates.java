package com.fidd.data.model;

import jakarta.persistence.*;

@Entity
@Table(name = "fidd_key_candidates")
public class FiddKeyCandidates {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Column(name = "footprint_base64", nullable = false, length = 2048)
    private String footprintBase64;

    @Lob
    @Column(name = "candidates_blob")
    private byte[] candidatesBlob;

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

    public String getFootprintBase64() {
        return footprintBase64;
    }

    public void setFootprintBase64(String footprintBase64) {
        this.footprintBase64 = footprintBase64;
    }

    public byte[] getCandidatesBlob() {
        return candidatesBlob;
    }

    public void setCandidatesBlob(byte[] candidatesBlob) {
        this.candidatesBlob = candidatesBlob;
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
