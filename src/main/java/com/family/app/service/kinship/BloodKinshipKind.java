package com.family.app.service.kinship;

/**
 * Mã nội bộ quan hệ <strong>máu</strong> (cha–con), góc {@code viewer} → {@code other}.
 * Các trường hợp còn lại (lệch thế hệ bàng phụ, anh em họ) xử lý ở tầng trên sau {@link BloodKinshipKind#UNKNOWN}.
 */
public enum BloodKinshipKind {
    OTHER_IS_ANCESTOR,
    OTHER_IS_DESCENDANT,
    SIBLING,
    OTHER_IS_UNCLE_OR_AUNT,
    VIEWER_IS_UNCLE_OR_AUNT_OF_OTHER,
    UNKNOWN
}
