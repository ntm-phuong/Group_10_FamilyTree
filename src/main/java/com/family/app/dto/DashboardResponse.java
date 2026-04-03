package com.family.app.dto;

import com.family.app.model.NewsEvent;
import com.family.app.model.User;
import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class DashboardResponse {
    private long totalMembers;
    private int totalGenerations;
    private long maleCount;
    private long femaleCount;
    private long totalNews;

    private Map<Integer, Long> generationDistribution; // <Đời thứ, Số lượng>
    private List<User> newMembers; // 5 thành viên mới nhất
    private List<NewsEvent> recentNews; // Tin tức mới nhất
}