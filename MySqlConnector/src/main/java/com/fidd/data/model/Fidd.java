package com.fidd.data.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.util.List;

@Entity
@Table(name = "fidd")
public class Fidd {
    @Id
    private String name;

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
