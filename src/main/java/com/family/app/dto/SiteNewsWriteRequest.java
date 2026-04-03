package com.family.app.dto;

import lombok.Data;

/**
 * Tạo / cập nhật tin công khai toàn site (family null).
 */
@Data
public class SiteNewsWriteRequest {
    private String title;
    /** Một trong {@link com.family.app.model.NewsCategory} — EVENT, ANNOUNCEMENT, … */
    private String publicCategory;
    private String summary;
    private String content;
    private String coverImage;
    private Boolean featured;
    /** true = lưu nháp (DRAFT), false/ null = đăng (PUBLIC_SITE khi tạo mới). */
    private Boolean draft;
    /** Chỉ khi sửa: PUBLIC_SITE | DRAFT */
    private String visibility;
    private String slug;
}
