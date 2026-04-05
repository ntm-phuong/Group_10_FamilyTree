package com.family.app.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "news_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsEvent {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String title;

    /** Slug cho URL /news/{slug} khi visibility = PUBLIC_SITE hoặc FAMILY_ONLY; duy nhất khi có giá trị. */
    @Column(length = 500, unique = true)
    private String slug;

    private String summary;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(length = 1000)
    private String coverImage;

    /** Danh mục tab trên trang /news (EVENT, ANNOUNCEMENT, …). */
    @Enumerated(EnumType.STRING)
    @Column(name = "public_category", length = 32)
    private NewsCategory publicCategory;

    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String location;
    private Integer remindBefore;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    @Builder.Default
    private NewsVisibility visibility = NewsVisibility.FAMILY_ONLY;

    @Column(nullable = false)
    @Builder.Default
    private boolean featured = false;

    @Column(nullable = false)
    @Builder.Default
    private int viewCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void ensureId() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }
}
