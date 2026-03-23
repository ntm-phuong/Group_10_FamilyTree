package com.family.app.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class NewsArticle {
    private final Long id;
    private final String slug;
    private final String title;
    private final String summary;
    private final String content;
    private final Category category;
    private final String coverImage;
    private final String authorName;
    private final boolean featured;
    private final int viewCount;
    private final LocalDate publishedDate;

    public enum Category {
        ANNOUNCEMENT("Thong bao"),
        EVENT("Su kien"),
        MEMORIAL("Tuong niem"),
        ACHIEVEMENT("Thanh tich"),
        HISTORY("Lich su"),
        OTHER("Khac");

        private final String label;

        Category(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public static Category fromParam(String raw) {
            if (raw == null || raw.isBlank()) {
                return null;
            }
            try {
                return Category.valueOf(raw.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
    }
}
