package com.family.app.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @Column(name = "permission_id", length = 36)
    private String permissionId;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    private String description;

    @PrePersist
    public void ensureId() {
        if (this.permissionId == null) {
            this.permissionId = UUID.randomUUID().toString();
        }
    }
}