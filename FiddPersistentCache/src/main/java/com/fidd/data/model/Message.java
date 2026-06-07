package com.fidd.data.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "message")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long number;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fidd_name", nullable = false)
    private Fidd fidd;

    @Column(name = "metadata_range")
    private String metadataRange;

    @OneToOne(mappedBy = "message", cascade = CascadeType.ALL)
    private UnencryptedFiddKey unencryptedFiddKey;

    @Column(name = "message_size")
    private Long messageSize;

    @Column(name = "fidd_key_count")
    private Long fiddKeySignatureCount;

    @Column(name = "signature_count")
    private Long fiddMesageSignatureCount;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL)
    private List<MetadataChunk> metadataChunks;

    @Column(name = "last_access_time")
    private Long lastAccessTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNumber() {
        return number;
    }

    public void setNumber(Long number) {
        this.number = number;
    }

    public Fidd getFidd() {
        return fidd;
    }

    public void setFidd(Fidd fidd) {
        this.fidd = fidd;
    }

    public String getMetadataRange() {
        return metadataRange;
    }

    public void setMetadataRange(String metadataRange) {
        this.metadataRange = metadataRange;
    }

    public UnencryptedFiddKey getUnencryptedFiddKey() {
        return unencryptedFiddKey;
    }

    public void setUnencryptedFiddKey(UnencryptedFiddKey unencryptedFiddKey) {
        this.unencryptedFiddKey = unencryptedFiddKey;
    }

    public Long getFiddKeySignatureCount() {
        return fiddKeySignatureCount;
    }

    public void setFiddKeySignatureCount(Long fiddKeySignatureCount) {
        this.fiddKeySignatureCount = fiddKeySignatureCount;
    }

    public Long getFiddMesageSignatureCount() {
        return fiddMesageSignatureCount;
    }

    public void setFiddMesageSignatureCount(Long fiddMesageSignatureCount) {
        this.fiddMesageSignatureCount = fiddMesageSignatureCount;
    }

    public Long getMessageSize() {
        return messageSize;
    }

    public void setMessageSize(Long messageSize) {
        this.messageSize = messageSize;
    }

    public List<MetadataChunk> getMetadataChunks() {
        return metadataChunks;
    }

    public void setMetadataChunks(List<MetadataChunk> metadataChunks) {
        this.metadataChunks = metadataChunks;
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
