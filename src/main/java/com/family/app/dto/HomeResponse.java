package com.family.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeResponse {
    // Thống kê
    private long totalMembers;
    private int totalGenerations;
    private long maleCount;
    private long femaleCount;

    // Thông tin dòng họ
    /** {@code family_id} tổ tông (gốc cây) — dùng cho link gia phả / lọc FE. */
    private String familyId;
    private String familyName;
    private String description;

    // Danh sách hiển thị
    private List<NewsResponse> upcomingEvents;
    private List<NewsResponse> latestNews;
}