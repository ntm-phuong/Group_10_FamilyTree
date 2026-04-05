package com.family.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    // Thống kê tổng quát (4 ô trên cùng)
    private long totalMembers;
    private long livingMembers;
    private long deceasedMembers;
    private long totalGenerations;
    private long totalNews;
    private long upcomingEvents;

    // Phân bố thế hệ (Biểu đồ cột bên trái)
    private Map<Integer, Long> generationDistribution;

    // Hoạt động & Danh sách (Các bảng phía dưới)
    private List<UserSummaryDTO> newMembers; // 4 thành viên mới thêm
    private List<NewsEventSummaryDTO> recentNews; // 5 bài viết mới nhất

}
