package com.family.app.service;

import com.family.app.dto.ClanMemberStatistics;
import com.family.app.dto.HomeResponse;
import com.family.app.model.Family;
import com.family.app.repository.FamilyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FamilyService {

    @Autowired
    private FamilyRepository familyRepository;

    @Autowired
    private MemberService memberService;

    @Autowired
    private NewsService newsService;

    /**
     * Trang chủ /home — thống kê thành viên dùng chung {@link MemberService#getClanMemberStatistics}
     * với {@link MemberService#getFamilyTreeData} (cùng phạm vi cây, cùng truy vấn đếm).
     */
    @Transactional(readOnly = true)
    public HomeResponse getHomeData(String familyId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new RuntimeException("Dòng họ không tồn tại"));

        ClanMemberStatistics stats = memberService.getClanMemberStatistics(family.getFamilyId());

        return HomeResponse.builder()
                .familyId(family.getFamilyId())
                .familyName(family.getFamilyName())
                .description(family.getDescription())
                .totalMembers(stats.totalMembers())
                .totalGenerations(stats.totalGenerations())
                .maleCount(stats.maleCount())
                .femaleCount(stats.femaleCount())
                .upcomingEvents(newsService.getUpcomingEvents(familyId, "cat-002"))
                .latestNews(newsService.getLatestNewsForHome(familyId))
                .build();
    }
}
