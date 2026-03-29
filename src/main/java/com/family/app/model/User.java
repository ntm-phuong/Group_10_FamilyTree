package com.family.app.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String email;
    private String password;
    private String gender;
    private LocalDate dob;
    private LocalDate dod; // Ngày mất

    private String hometown; // Quê quán
    private String currentAddress; // Địa chỉ hiện tại
    private String occupation; // Nghề nghiệp
    private String phoneNumber;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String avatar;

    private Integer status; // 0: Pending, 1: Active

    @Column(name = "order_in_family")
    private Integer orderInFamily;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id")
    private Family family;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void ensureId() {
        if (this.userId == null) {
            this.userId = UUID.randomUUID().toString();
        }
    }

}