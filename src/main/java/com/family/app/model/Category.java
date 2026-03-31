package com.family.app.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @Column(name = "category_id", length = 36)
    private String categoryId; // Đây là Primary Key

    @Column(nullable = false, length = 100)
    private String name;

    @PrePersist
    public void ensureId() {
        if (this.categoryId == null) {
            this.categoryId = UUID.randomUUID().toString();
        }
    }
}