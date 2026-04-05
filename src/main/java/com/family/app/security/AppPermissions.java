package com.family.app.security;

/**
 * Tên quyền (GrantedAuthority) — khớp bảng {@code permissions.name}.
 *
 * <p>Tóm tắt: tin/member <em>trong family</em> ({@link #MANAGE_FAMILY_NEWS}, {@link #MANAGE_FAMILY_MEMBERS})
 * bị giới hạn bởi {@link com.family.app.service.FamilyScopeService} trừ khi có {@link #MANAGE_CLAN}.</p>
 */
public final class AppPermissions {

    private AppPermissions() {
    }

    /**
     * Tin công khai toàn site (trang quản trị /site-news-manage) — không gắn một chi cụ thể.
     */
    public static final String MANAGE_SITE_NEWS = "MANAGE_SITE_NEWS";

    /** Tin / sự kiện theo {@code family_id} — mặc định trong phạm vi nhánh user (xem FamilyScopeService). */
    public static final String MANAGE_FAMILY_NEWS = "MANAGE_FAMILY_NEWS";

    /** Thành viên & chi — mặc định trong phạm vi nhánh user. */
    public static final String MANAGE_FAMILY_MEMBERS = "MANAGE_FAMILY_MEMBERS";

    /**
     * Bỏ giới hạn nhánh: được thao tác trên <strong>mọi chi</strong> thuộc cây phả hệ gốc {@code app.clan.family-id}
     * (mọi descendant của tổ tông cấu hình). Chỉ nên gán cho role {@code ADMIN} (một tài khoản).
     */
    public static final String MANAGE_CLAN = "MANAGE_CLAN";
}
