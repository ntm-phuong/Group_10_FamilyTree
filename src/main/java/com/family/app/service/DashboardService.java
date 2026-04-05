package com.family.app.service;

import com.family.app.dto.DashboardResponse;
import com.family.app.model.User;
import com.family.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private UserRepository userRepository;

    public DashboardResponse getFamilyHeadDashboard(User currentUser) {
        if (currentUser.getFamily() == null) {
            return new DashboardResponse();
        }

        String familyId = currentUser.getFamily().getFamilyId();
        DashboardResponse dto = new DashboardResponse();

        // 1. Lấy các con số tổng quát từ Repository
        dto.setTotalMembers((int) userRepository.countByFamily_FamilyId(familyId));

        // Đếm dựa trên logic dod (Ngày mất) trong Entity User
        // Bạn có thể viết thêm countByFamily_FamilyIdAndDodIsNull trong Repo nếu muốn tối ưu
        long living = userRepository.findByFamily_FamilyIdOrderByOrderInFamilyAsc(familyId)
                .stream().filter(User::isAlive).count();

        dto.setLivingMembers((int) living);
        dto.setDeceasedMembers(dto.getTotalMembers() - (int) living);

        // 2. Lấy số đời cao nhất
        Integer maxGen = userRepository.findMaxGenerationByFamilyId(familyId);
        dto.setTotalGenerations(maxGen != null ? maxGen : 0);

        // 3. Thống kê phân bổ đời (Ví dụ: Đời 1: 2 người, Đời 2: 5 người...)
        // Dùng TreeMap để tự động sắp xếp theo thứ tự Đời 1 -> 2 -> 3
        Map<Integer, Long> genDist = userRepository.findByFamily_FamilyIdOrderByOrderInFamilyAsc(familyId)
                .stream()
                .filter(u -> u.getGeneration() != null)
                .collect(Collectors.groupingBy(
                        User::getGeneration, // Key là Integer (đời thứ mấy)
                        TreeMap::new,        // Sắp xếp theo thứ tự đời
                        Collectors.counting() // Value trả về kiểu Long (số lượng)
                ));
        dto.setGenerationDistribution(genDist);

        return dto;
    }
}