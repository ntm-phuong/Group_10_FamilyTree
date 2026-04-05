package com.family.app.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cấu hình hệ thống một dòng họ (demo / triển khai đơn giản).
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.clan")
public class AppClanProperties {

    /** Mã dòng họ duy nhất (FK users.family_id, news_events, …). */
    private String familyId = "fam-nguyen-001";

    /** Tên hiển thị trên tin tức / gia phả (mặc định an toàn encoding). */
    private String displayName = ClanBranding.DISPLAY_NAME;
}
