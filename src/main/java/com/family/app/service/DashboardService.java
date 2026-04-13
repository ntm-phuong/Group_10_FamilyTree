package com.family.app.service;

import com.family.app.dto.ClanMemberStatistics;
import com.family.app.dto.DashboardResponse;
import com.family.app.dto.NewsEventSummaryDTO;
import com.family.app.dto.UserSummaryDTO;
import com.family.app.model.NewsEvent;
import com.family.app.model.User;
import com.family.app.repository.NewsEventRepository;
import com.family.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FamilyScopeService familyScopeService;
    @Autowired
    private NewsEventRepository newsEventRepository;
    @Autowired
    private MemberService memberService;

    /**
     * Thống kê dashboard trưởng họ — <strong>cùng phạm vi cây</strong> với trang {@code /home}
     * ({@link com.family.app.service.FamilyService#getHomeData}): tổ tông của user + mọi chi con.
     * Không dùng {@link FamilyScopeService#manageableFamilyIdsForMembers} (chỉ nhánh xuống từ chi hiện tại)
     * để tránh lệch số với trang chủ.
     */
    @Transactional(readOnly = true)
    public DashboardResponse getFamilyHeadDashboard(User currentUser) {
        if (currentUser == null || currentUser.getUserId() == null) {
            return new DashboardResponse();
        }
        Set<String> scopeIds = familyScopeService.manageableFamilyIdsForHeadDashboardNews(currentUser.getUserId());
        if (scopeIds.isEmpty()) {
            return new DashboardResponse();
        }
        List<String> scopeIdList = new ArrayList<>(scopeIds);

        List<User> users = userRepository.findByFamily_FamilyIdInOrderByGenerationAscOrderInFamilyAsc(scopeIds);
        DashboardResponse dto = new DashboardResponse();

        ClanMemberStatistics memberStats = memberService.computeStatisticsForScope(scopeIds);
        dto.setTotalMembers(memberStats.totalMembers());
        long living = users.stream().filter(User::isAlive).count();
        dto.setLivingMembers(living);
        dto.setDeceasedMembers(memberStats.totalMembers() - living);

        dto.setTotalGenerations(memberStats.totalGenerations());

        Map<Integer, Long> genDist = users.stream()
                .filter(u -> u.getGeneration() != null)
                .collect(Collectors.groupingBy(
                        User::getGeneration,
                        TreeMap::new,
                        Collectors.counting()));
        dto.setGenerationDistribution(genDist);

        long publishedNews = 0;
        long draftNews = 0;
        long totalNews = 0;
        if (!scopeIdList.isEmpty()) {
            publishedNews = newsEventRepository.countVisibleByFamilyFamilyIdIn(scopeIdList);
            draftNews = newsEventRepository.countDraftByFamilyFamilyIdIn(scopeIdList);
            totalNews = newsEventRepository.countByFamilyFamilyIdIn(scopeIdList);
        }
        dto.setTotalNews(totalNews);
        dto.setPublishedNewsCount(publishedNews);
        dto.setDraftNewsCount(draftNews);

        // -- Thành vên mới (5 người mới tạo gần nhất)
        List<UserSummaryDTO> newMembers = users.stream()
                .filter(u -> u.getCreatedAt() != null)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(5)
                .map(u -> new UserSummaryDTO(
                        u.getFullName(),
                        u.getGeneration() != null ? u.getGeneration() : 0,
                        u.getGender()))
                .collect(Collectors.toList());
        dto.setNewMembers(newMembers);

        // --- Tin tức mới nhất (5 bài) — mọi chi trong phạm vi tin dashboard ---
        List<NewsEvent> newsEvents = scopeIdList.isEmpty()
                ? List.of()
                : newsEventRepository.findByFamily_FamilyIdInOrderByCreatedAtDesc(
                        scopeIdList,
                        PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<NewsEventSummaryDTO> recentNews = newsEvents.stream()
                .map(n -> new NewsEventSummaryDTO(
                        n.getTitle(),
                        n.getCreatedAt(),
                        n.getVisibility() != null ? n.getVisibility().name() : "FAMILY_ONLY"))
                .collect(Collectors.toList());
        dto.setRecentNews(recentNews);

        return dto;
    }
}
