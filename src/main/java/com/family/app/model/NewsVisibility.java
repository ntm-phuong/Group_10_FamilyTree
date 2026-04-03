package com.family.app.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Phạm vi hiển thị tin/sự kiện.
 */
@Getter
@RequiredArgsConstructor
public enum NewsVisibility {
    /** Nháp — chỉ quản trị / tác giả (không công khai). */
    DRAFT("Nháp"),
    /** Chỉ trong phạm vi một dòng họ (tin nội bộ). */
    FAMILY_ONLY("Nội bộ dòng họ"),
    /** Hiển thị trên trang /news công khai. */
    PUBLIC_SITE("Công khai");

    private final String label;
}
