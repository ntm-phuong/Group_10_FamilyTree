package com.family.app.service;

import com.family.app.dto.HomeResponse;
import com.family.app.dto.NewsResponse;
import com.family.app.model.Family;
import com.family.app.repository.FamilyRepository;
import com.family.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class FamilyService {

    @Autowired
    private FamilyRepository familyRepository;

    @Autowired
    private UserRepository userRepository; // Đổi từ MemberRepository sang UserRepository

    @Autowired
    private NewsService newsService;

    @Transactional(readOnly = true)
    public HomeResponse getHomeData(String familyId) {
        // 1. Lấy thông tin dòng họ gốc
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new RuntimeException("Dòng họ không tồn tại"));

        // 2. Lấy danh sách familyId bao gồm cả chi/nhánh con (đệ quy)
        List<String> familyIds = familyRepository.findDescendantFamilyIds(familyId);
        if (familyIds == null || familyIds.isEmpty()) {
            familyIds = Collections.singletonList(familyId);
        }

        // 3. Thống kê trên toàn bộ phạm vi familyIds
        long totalMembers = userRepository.countByFamily_FamilyIdIn(familyIds);
        long maleCount = userRepository.countByFamily_FamilyIdInAndGender(familyIds, "MALE");
        long femaleCount = userRepository.countByFamily_FamilyIdInAndGender(familyIds, "FEMALE");

        // Lấy số đời lớn nhất trên phạm vi
        Integer maxGen = userRepository.findMaxGenerationByFamilyIds(familyIds);
        int totalGenerations = (maxGen != null) ? maxGen : 0;

        // 4. Lấy sự kiện / tin tức trên phạm vi nhiều chi
        List<NewsResponse> upcomingEvents = newsService.getUpcomingEventsForFamilies(familyIds, "cat-002");
        List<NewsResponse> latestNews = newsService.getLatestNewsForFamilies(familyIds);

        return HomeResponse.builder()
                .familyName(family.getFamilyName())
                .description(family.getDescription())
                .totalMembers(totalMembers)
                .totalGenerations(totalGenerations)
                .maleCount(maleCount)
                .femaleCount(femaleCount)
                .upcomingEvents(upcomingEvents)
                .latestNews(latestNews)
                .build();
    }
}