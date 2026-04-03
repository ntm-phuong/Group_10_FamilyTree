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
    /** Chỉ trong phạm vi một dòng họ (API family-head). */
    FAMILY_ONLY("Nội bộ dòng họ"),
    /** Hiển thị trên trang /news công khai. */
    PUBLIC_SITE("Công khai");

    private final String label;
}
