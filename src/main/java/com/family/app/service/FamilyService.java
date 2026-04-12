package com.family.app.service;

import com.family.app.dto.HomeResponse;
import com.family.app.model.Family;
import com.family.app.repository.FamilyRepository;
import com.family.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Set;

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

        // 2. Cùng phạm vi cây gia phả: gốc + mọi chi con (parent_family_id)
        Set<String> scopeIds = subtreeFamilyIds(familyId);
        if (scopeIds.isEmpty()) {
            scopeIds.add(familyId);
        }

        long totalMembers = userRepository.countByFamily_FamilyIdIn(scopeIds);
        long maleCount = userRepository.countByFamily_FamilyIdInAndGender(scopeIds, "MALE");
        long femaleCount = userRepository.countByFamily_FamilyIdInAndGender(scopeIds, "FEMALE");

        Integer maxGen = userRepository.findMaxGenerationByFamilyIdIn(scopeIds);
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

    /** Gốc {@code familyId} và mọi chi con (đồng bộ với {@link MemberService#getFamilyTreeData}). */
    private Set<String> subtreeFamilyIds(String rootFamilyId) {
        Set<String> ids = new LinkedHashSet<>();
        ArrayDeque<String> q = new ArrayDeque<>();
        q.add(rootFamilyId);
        while (!q.isEmpty()) {
            String id = q.poll();
            if (!ids.add(id)) {
                continue;
            }
            for (Family c : familyRepository.findByParentFamily_FamilyId(id)) {
                q.add(c.getFamilyId());
            }
        }
        return ids;
    }
}