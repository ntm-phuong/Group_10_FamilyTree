package com.family.app.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cấu hình dòng họ gốc — giá trị thật chỉ đến từ {@code application.properties} / biến môi trường, không hardcode trong code.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.clan")
public class AppClanProperties {

    /** Mã {@code families.family_id} gốc (bắt buộc cấu hình khi triển khai). */
    private String familyId = "";

    /** Tên hiển thị trên tin tức / gia phả (mặc định an toàn encoding). */
    private String displayName = ClanBranding.DISPLAY_NAME;
}
