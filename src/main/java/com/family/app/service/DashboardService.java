package com.family.app.service;

import com.family.app.dto.DashboardResponse;
import com.family.app.model.User;
import com.family.app.repository.NewsEventRepository;
import com.family.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    /**
     * Thống kê theo phạm vi quản lý thành viên ({@link FamilyScopeService#manageableFamilyIdsForMembers}):
     * trưởng họ = nhánh của {@code user.family}; có {@code MANAGE_CLAN} / ADMIN = toàn bộ chi từ tổ tông xuống.
     */
    @Transactional(readOnly = true)
    public DashboardResponse getFamilyHeadDashboard(User currentUser) {
        if (currentUser == null || currentUser.getUserId() == null) {
            return new DashboardResponse();
        }
        Set<String> familyIds = familyScopeService.manageableFamilyIdsForMembers(currentUser.getUserId());
        if (familyIds.isEmpty()) {
            return new DashboardResponse();
        }

        List<User> users = userRepository.findByFamily_FamilyIdInOrderByGenerationAscOrderInFamilyAsc(familyIds);
        DashboardResponse dto = new DashboardResponse();

        int total = users.size();
        dto.setTotalMembers(total);
        long living = users.stream().filter(User::isAlive).count();
        dto.setLivingMembers(living);
        dto.setDeceasedMembers(total - living);

        int maxGen = users.stream()
                .map(User::getGeneration)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
        dto.setTotalGenerations(maxGen);

        Map<Integer, Long> genDist = users.stream()
                .filter(u -> u.getGeneration() != null)
                .collect(Collectors.groupingBy(
                        User::getGeneration,
                        TreeMap::new,
                        Collectors.counting()));
        dto.setGenerationDistribution(genDist);

        dto.setTotalNews(newsEventRepository.countVisibleByFamilyFamilyIdIn(familyIds));

        return dto;
    }
}
