package com.family.app.config;

/**
 * Chuỗi hiển thị dòng họ — dùng escape {@code \\uXXXX} để luôn đúng UTF-8,
 * tránh mojibake kiểu {@code Há» Nguyá»n} khi file .properties / build không UTF-8.
 */
public final class ClanBranding {

    private ClanBranding() {
    }

    /** Họ Nguyễn Đông Anh */
    public static final String DISPLAY_NAME =
            "H\u1ecd Nguy\u1ec5n \u0110\u00f4ng Anh";

    /**
     * Mô tả tổ tông (gốc app.clan.family-id).
     *
     * @param displayName thường lấy từ {@code AppClanProperties#getDisplayName()}
     */
    public static String rootClanDescription(String displayName) {
        return "T\u1ed5 t\u00f4ng chung \u2014 " + displayName
                + " (H\u00e0 N\u1ed9i). C\u00e1c chi \u0111\u0103ng k\u00fd ph\u1ee5 thu\u1ed9c theo parent_family_id.";
    }
}
