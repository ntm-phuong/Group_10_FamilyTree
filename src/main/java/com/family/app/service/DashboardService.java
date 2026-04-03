package com.family.app.service;

import com.family.app.dto.HomeResponse;
import com.family.app.dto.NewsResponse;
import com.family.app.model.NewsEvent;
import com.family.app.repository.NewsEventRepository;
import com.family.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NewsEventRepository newsEventRepository;

    public HomeResponse getDashboardData(String familyId) {
        // 1. Thống kê số lượng thành viên
        long total = userRepository.countByFamily_FamilyId(familyId);
        long male = userRepository.countByFamily_FamilyIdAndGender(familyId, "Nam");

        // 2. Lấy số đời cao nhất
        Integer maxGen = userRepository.findMaxGenerationByFamilyId(familyId);
        int totalGenerations = (maxGen != null) ? maxGen : 0;

        // 3. Lấy 5 tin tức mới nhất và Map sang NewsResponse
        List<NewsEvent> newsEntities = newsEventRepository.findTop5ByFamily_FamilyIdOrderByCreatedAtDesc(familyId);

        List<NewsResponse> newsResponses = newsEntities.stream()
                .map(event -> NewsResponse.builder()
                        .id(event.getId()) // SỬA TẠI ĐÂY: Dùng .id() vì DTO khai báo là 'id'
                        .title(event.getTitle())
                        .summary(event.getContent()) // Giả định nội dung làm tóm tắt
                        .content(event.getContent())
                        .createdAt(event.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        // 4. Trả về HomeResponse phẳng khớp với Controller
        return HomeResponse.builder()
                .totalMembers(total)
                .totalGenerations(totalGenerations)
                .maleCount(male)
                .femaleCount(total - male)
                .familyName("Dòng họ Nguyễn")
                .latestNews(newsResponses)
                .build();
    }
}