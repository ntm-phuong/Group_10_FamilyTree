package com.family.app.security;

/**
 * Tên quyền (GrantedAuthority) — khớp bảng {@code permissions.name}.
 */
public final class AppPermissions {

    private AppPermissions() {
    }

    /**
     * Tên quyền cũ / dự phòng — tin công khai trên /news gắn theo từng {@code family_id};
     * seed không gán quyền “toàn hệ”.
     */
    public static final String MANAGE_SITE_NEWS = "MANAGE_SITE_NEWS";

    /** Tin / sự kiện — phạm vi: chi của tài khoản và mọi chi con (nhánh dưới). */
    public static final String MANAGE_FAMILY_NEWS = "MANAGE_FAMILY_NEWS";

    /** Thành viên & thông tin dòng họ (danh sách chi, sửa tên chi, …) — cùng phạm vi cây con. */
    public static final String MANAGE_FAMILY_MEMBERS = "MANAGE_FAMILY_MEMBERS";
}
