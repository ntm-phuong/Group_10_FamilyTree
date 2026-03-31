package com.family.app.service;

import com.family.app.dto.FamilyTreeResponse;
import com.family.app.model.Relationship;
import com.family.app.model.User;
import com.family.app.repository.FamilyRepository;
import com.family.app.repository.RelationshipRepository;
import com.family.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final UserRepository userRepository;
    private final RelationshipRepository relationshipRepository;
    private final FamilyRepository familyRepository;

    public FamilyTreeResponse getFamilyTreeDataForPublic(String familyId) {
        String resolvedFamilyId = familyId;
        if (resolvedFamilyId == null || resolvedFamilyId.isBlank()) {
            resolvedFamilyId = familyRepository.findAll(PageRequest.of(0, 1))
                    .stream()
                    .findFirst()
                    .map(family -> family.getFamilyId())
                    .orElseThrow(() -> new IllegalStateException("No family data found"));
        }
        return getFamilyTreeData(resolvedFamilyId);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public FamilyTreeResponse getFamilyTreeData(String familyId) {
        // 1. Lấy thông tin dòng họ
        String familyName = familyRepository.findById(familyId)
                .map(f -> f.getFamilyName()).orElse("Gia phả");

        // 2. Lấy tất cả thành viên và quan hệ
        List<User> users = userRepository.findByFamily_FamilyIdOrderByOrderInFamilyAsc(familyId);
        List<Relationship> relationships = relationshipRepository.findAllByFamilyId(familyId);

        // 3. Chuyển đổi User thành MemberNode và gắn logic quan hệ
        List<FamilyTreeResponse.MemberNode> nodes = users.stream().map(user -> {
            FamilyTreeResponse.MemberNode node = FamilyTreeResponse.MemberNode.builder()
                    .user_id(user.getUserId())
                    .name(user.getFullName())
                    .gender(user.getGender())
                    .birthYear(user.getDob() != null ? user.getDob().getYear() : null)
                    .deathYear(user.getDod() != null ? user.getDod().getYear() : null)
                    .occupation(user.getOccupation())
                    .generation(user.getGeneration())
                    .branch(user.getBranch())
                    .orderInFamily(user.getOrderInFamily())
                    .avatar(user.getAvatar())
                    .isDead(user.getDod() != null)
                    .build();

            // Duyệt quan hệ để tìm người thân cho Node này
            for (Relationship rel : relationships) {
                String p1 = rel.getPerson1().getUserId();
                String p2 = rel.getPerson2().getUserId();
                String current = user.getUserId();

                if ("SPOUSE".equals(rel.getRelType())) {
                    if (p1.equals(current)) node.setSpouseId(p2);
                    else if (p2.equals(current)) node.setSpouseId(p1);
                }
                else if ("PARENT_CHILD".equals(rel.getRelType())) {
                    // p1 là cha/mẹ, p2 là con
                    if (p2.equals(current)) {
                        if ("MALE".equals(rel.getPerson1().getGender())) {
                            node.setFatherId(p1);
                        } else {
                            node.setMotherId(p1);
                        }
                    }
                }
            }
            return node;
        }).collect(Collectors.toList());

        // Tính tổng số đời (Max generation)
        Integer maxGen = users.stream()
                .map(u -> u.getGeneration())
                .filter(g -> g != null)
                .max(Integer::compare).orElse(1);

        return FamilyTreeResponse.builder()
                .familyName(familyName)
                .totalGenerations(maxGen)
                .members(nodes)
                .build();
    }
}