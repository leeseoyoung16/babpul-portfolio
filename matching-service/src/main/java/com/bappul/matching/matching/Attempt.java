package com.bappul.matching.matching;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "attempts")
public class Attempt {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String userId;

    @Column(columnDefinition = "json")
    private String prefsJson;

    @Column(nullable = false)
    private LocalTime windowStart;

    @Column(nullable = false)
    private LocalTime windowEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.PENDING;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    public enum Status { PENDING, MATCHED, CANCELED, EXPIRED }

    public Attempt() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getPrefsJson() { return prefsJson; }
    public void setPrefsJson(String prefsJson) { this.prefsJson = prefsJson; }
    public LocalTime getWindowStart() { return windowStart; }
    public void setWindowStart(LocalTime windowStart) { this.windowStart = windowStart; }
    public LocalTime getWindowEnd() { return windowEnd; }
    public void setWindowEnd(LocalTime windowEnd) { this.windowEnd = windowEnd; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
