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

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL)
    private List<FiddKey> fiddKeys;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL)
    private List<Signature> signatures;

    @OneToOne(mappedBy = "message", cascade = CascadeType.ALL)
    private MessageSize messageSize;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL)
    private List<MetadataChunk> metadataChunks;

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

    public List<FiddKey> getFiddKeys() {
        return fiddKeys;
    }

    public void setFiddKeys(List<FiddKey> fiddKeys) {
        this.fiddKeys = fiddKeys;
    }

    public List<Signature> getSignatures() {
        return signatures;
    }

    public void setSignatures(List<Signature> signatures) {
        this.signatures = signatures;
    }

    public MessageSize getMessageSize() {
        return messageSize;
    }

    public void setMessageSize(MessageSize messageSize) {
        this.messageSize = messageSize;
    }

    public List<MetadataChunk> getMetadataChunks() {
        return metadataChunks;
    }

    public void setMetadataChunks(List<MetadataChunk> metadataChunks) {
        this.metadataChunks = metadataChunks;
    }
}
