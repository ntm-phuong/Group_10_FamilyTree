package com.family.app.service;

import com.family.app.dto.HomeResponse;
import com.family.app.model.Family;
import com.family.app.repository.FamilyRepository;
import com.family.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        // 1. Lấy thông tin dòng họ
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new RuntimeException("Dòng họ không tồn tại"));

        // 2. Lấy số liệu thống kê thực tế từ UserRepository
        long totalMembers = userRepository.countByFamily_FamilyId(familyId);
        long maleCount = userRepository.countByFamily_FamilyIdAndGender(familyId, "MALE");
        long femaleCount = userRepository.countByFamily_FamilyIdAndGender(familyId, "FEMALE");

        // Lấy số đời lớn nhất
        Integer maxGen = userRepository.findMaxGenerationByFamilyId(familyId);
        int totalGenerations = (maxGen != null) ? maxGen : 0;

        return HomeResponse.builder()
                .familyName(family.getFamilyName())
                .description(family.getDescription())
                .totalMembers(totalMembers)
                .totalGenerations(totalGenerations)
                .maleCount(maleCount)
                .femaleCount(femaleCount)
                .upcomingEvents(newsService.getUpcomingEvents(familyId, "cat-002"))
                .latestNews(newsService.getLatestNewsForHome(familyId))
                .build();
    }
}