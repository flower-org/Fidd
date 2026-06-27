package com.fidd.data.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "fidd")
public class Fidd {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", unique = true)
    private String name;

    @Nullable
    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;

    @Column(name = "last_update_time")
    private Long lastUpdateTime;

    @Nullable
    @Column(name = "fidd_info", columnDefinition = "LONGTEXT")
    private String fiddInfo;

    @Nullable
    @Column(name = "certificate", columnDefinition = "LONGTEXT")
    private String certificate;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    @OneToMany(mappedBy = "fidd", cascade = CascadeType.ALL)
    private List<Message> messages;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @OneToMany(mappedBy = "fidd", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<FiddTag> tags;

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    public List<FiddTag> getTags() {
        return tags;
    }

    public void setTags(List<FiddTag> tags) {
        this.tags = tags;
    }

    @Nullable
    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(@Nullable String certificate) {
        this.certificate = certificate;
    }

    @Nullable
    public String getFiddInfo() {
        return fiddInfo;
    }

    public void setFiddInfo(@Nullable String fiddInfo) {
        this.fiddInfo = fiddInfo;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    @PrePersist
    @PreUpdate
    public void updateLastAccessTime() {
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public Long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(Long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }
}
