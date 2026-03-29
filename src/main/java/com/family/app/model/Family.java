package com.family.app.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "families")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Family {

    @Id
    @Column(name = "family_id", length = 36)
    private String familyId;

    @Column(name = "family_name", nullable = false)
    private String familyName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "privacy_setting", length = 20)
    private String privacySetting; // e.g., "PUBLIC", "PRIVATE"

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void ensureId() {
        if (this.familyId == null) {
            this.familyId = UUID.randomUUID().toString();
        }
    }

    // Đã xóa sạch Getters/Setters thủ công!
}