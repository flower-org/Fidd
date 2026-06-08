package com.fidd.data.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import javax.annotation.Nullable;
import java.util.List;

@Entity
@Table(name = "fidd")
public class Fidd {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private java.util.UUID id;

    @Column(name = "name", unique = true)
    private String name;

    public java.util.UUID getId() {
        return id;
    }

    public void setId(java.util.UUID id) {
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

    @Column(name = "last_access_time")
    private Long lastAccessTime;

    @Nullable
    @Column(name = "info", columnDefinition = "LONGTEXT")
    private String info;

    @Nullable
    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;

    @OneToMany(mappedBy = "fidd", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<FiddTag> tags;

    @Nullable
    @Column(name = "certificate", columnDefinition = "LONGTEXT")
    private String certificate;

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
    public String getInfo() {
        return info;
    }

    public void setInfo(@Nullable String info) {
        this.info = info;
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
        this.lastAccessTime = System.currentTimeMillis();
    }

    public Long getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(Long lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }
}
