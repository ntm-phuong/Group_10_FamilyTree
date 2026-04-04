package com.family.app.service;

import com.family.app.dto.DashboardResponse;
import com.family.app.dto.NewsEventSummaryDTO;
import com.family.app.dto.UserSummaryDTO;
import com.family.app.model.NewsEvent;
import com.family.app.model.User;
import com.family.app.repository.NewsEventRepository;
import com.family.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    @Autowired private UserRepository userRepository;
    @Autowired private NewsEventRepository newsEventRepository;
    
    public DashboardResponse getFamilyHeadDashboard(String familyId) {
        // Lấy tất cả User thuộc Family để xử lý trong bộ nhớ (tối ưu cho dòng họ vừa và nhỏ)
        List<User> familyMembers = userRepository.findByFamily_FamilyIdOrderByOrderInFamilyAsc(familyId);

        // 1. Thống kê tổng quát
        long totalMembers = familyMembers.size();
        long livingMembers = familyMembers.stream().filter(User::isAlive).count();

        // 2. Phân bố thế hệ (Đếm số người theo từng đời)
        Map<Integer, Long> generationDist = familyMembers.stream()
                .collect(Collectors.groupingBy(User::getGeneration, Collectors.counting()));

        // 3. Lấy 5 tin tức mới nhất từ Repository có sẵn
        List<NewsEvent> topNews = newsEventRepository.findTop5ByFamily_FamilyIdOrderByCreatedAtDesc(familyId);

        return DashboardResponse.builder()
                .totalMembers(totalMembers)
                .livingMembers(livingMembers)
                .deceasedMembers(totalMembers - livingMembers)
                .totalGenerations(generationDist.size())
                .generationDistribution(generationDist)
                .newMembers(familyMembers.stream().limit(4).map(this::mapToUserSummary).collect(Collectors.toList()))
                .recentNews(topNews.stream().map(this::mapToNewsSummary).collect(Collectors.toList()))
                .build();
    }

    private UserSummaryDTO mapToUserSummary(User user) {
        // Lấy dữ liệu từ entity User để đổ vào DTO rút gọn
        return new UserSummaryDTO(
                user.getFullName(),
                user.getGeneration(), // Lấy từ field generation trong file bạn gửi
                user.getGender()      // Lấy để phân biệt màu sắc giao diện
        );
    }

    private NewsEventSummaryDTO mapToNewsSummary(NewsEvent news) {
        return new NewsEventSummaryDTO(news.getTitle(), news.getCreatedAt(), "Đã đăng");
    }
}