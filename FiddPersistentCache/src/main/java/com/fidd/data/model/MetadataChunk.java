package com.fidd.data.model;

import jakarta.persistence.*;

@Entity
@Table(name = "metadata_chunks")
public class MetadataChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Column(name = "range_from")
    private Long rangeFrom;

    @Column(name = "range_to")
    private Long rangeTo;

    @Column(name = "last_access_time")
    private Long lastAccessTime;

    @Lob
    @Column(name = "data")
    private byte[] data;

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

    public Long getRangeFrom() {
        return rangeFrom;
    }

    public void setRangeFrom(Long rangeFrom) {
        this.rangeFrom = rangeFrom;
    }

    public Long getRangeTo() {
        return rangeTo;
    }

    public void setRangeTo(Long rangeTo) {
        this.rangeTo = rangeTo;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
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
