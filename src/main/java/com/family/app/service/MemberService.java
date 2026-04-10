package com.family.app.service;

import com.family.app.dto.FamilyTreeResponse;
import com.family.app.dto.RelationshipCompareResponse;
import com.family.app.model.Relationship;
import com.family.app.model.User;
import com.family.app.repository.FamilyRepository;
import com.family.app.repository.RelationshipRepository;
import com.family.app.repository.UserRepository;
import com.family.app.config.AppClanProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.family.app.model.Family;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final UserRepository userRepository;
    private final RelationshipRepository relationshipRepository;
    private final FamilyRepository familyRepository;
    private final AppClanProperties clanProperties;

    public FamilyTreeResponse getFamilyTreeDataForPublic(String familyId) {
        String resolvedFamilyId = familyId;
        if (resolvedFamilyId == null || resolvedFamilyId.isBlank()) {
            resolvedFamilyId = clanProperties.getFamilyId();
        }
        if (!clanProperties.getFamilyId().equals(resolvedFamilyId)) {
            throw new IllegalStateException("Chỉ hỗ trợ dòng họ đã cấu hình.");
        }
        return getFamilyTreeData(resolvedFamilyId);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public FamilyTreeResponse getFamilyTreeData(String familyId) {
        // 1. Lấy thông tin dòng họ (gốc được yêu cầu — gồm cả các chi con)
        String familyName = familyRepository.findById(familyId)
                .map(f -> f.getFamilyName()).orElse("Gia phả");

        Set<String> scopeIds = subtreeFamilyIds(familyId);
        if (scopeIds.isEmpty()) {
            scopeIds.add(familyId);
        }

        // 2. Thành viên + quan hệ trên toàn bộ nhánh (root và các family phụ thuộc)
        List<User> users = userRepository.findByFamily_FamilyIdInOrderByGenerationAscOrderInFamilyAsc(scopeIds);
        List<Relationship> relationships = relationshipRepository.findAllWhereBothPersonsInFamilyIds(scopeIds);

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
                    .parentId(user.getParentId())
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

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public RelationshipCompareResponse compareRelationship(String familyId, String memberAId, String memberBId) {
        if (memberAId == null || memberBId == null || memberAId.isBlank() || memberBId.isBlank()) {
            throw new IllegalArgumentException("Bắt buộc truyền memberAId/memberBId");
        }

        String clanRoot = clanProperties.getFamilyId();
        Set<String> clanScope = subtreeFamilyIds(clanRoot);
        if (familyId != null && !familyId.isBlank() && !clanRoot.equals(familyId.trim())) {
            throw new IllegalArgumentException("Chỉ hỗ trợ dòng họ đã cấu hình.");
        }

        User ua = userRepository.findByIdWithFamily(memberAId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thành viên A"));
        User ub = userRepository.findByIdWithFamily(memberBId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thành viên B"));
        if (ua.getFamily() == null || ub.getFamily() == null) {
            throw new IllegalArgumentException("Thành viên chưa gắn dòng họ");
        }
        if (!clanScope.contains(ua.getFamily().getFamilyId())
                || !clanScope.contains(ub.getFamily().getFamilyId())) {
            throw new IllegalArgumentException("Thành viên không thuộc dòng họ đã cấu hình.");
        }

        List<User> users = userRepository.findByFamily_FamilyIdInOrderByGenerationAscOrderInFamilyAsc(clanScope);
        List<Relationship> relationships = relationshipRepository.findAllWhereBothPersonsInFamilyIds(clanScope);

        Map<String, User> userById = users.stream().collect(Collectors.toMap(User::getUserId, u -> u));
        if (!userById.containsKey(memberAId) || !userById.containsKey(memberBId)) {
            throw new IllegalArgumentException("Không tìm thấy thành viên trong dòng họ");
        }

        Map<String, Set<String>> parentsByChild = new HashMap<>();
        Map<String, String> fatherByChild = new HashMap<>();
        Map<String, String> motherByChild = new HashMap<>();
        Map<String, Set<String>> spousesByMember = new HashMap<>();
        for (Relationship rel : relationships) {
            String p1 = rel.getPerson1().getUserId();
            String p2 = rel.getPerson2().getUserId();
            if ("PARENT_CHILD".equals(rel.getRelType())) {
                parentsByChild.computeIfAbsent(p2, k -> new HashSet<>()).add(p1);
                User parent = rel.getPerson1();
                if (parent != null && parent.getGender() != null) {
                    if ("MALE".equalsIgnoreCase(parent.getGender())) {
                        fatherByChild.putIfAbsent(p2, p1);
                    } else if ("FEMALE".equalsIgnoreCase(parent.getGender())) {
                        motherByChild.putIfAbsent(p2, p1);
                    }
                }
            } else if ("SPOUSE".equals(rel.getRelType()) && rel.getEndDate() == null) {
                spousesByMember.computeIfAbsent(p1, k -> new HashSet<>()).add(p2);
                spousesByMember.computeIfAbsent(p2, k -> new HashSet<>()).add(p1);
            }
        }

        String relationshipAToB = inferRelationship(memberAId, memberBId, userById, parentsByChild, fatherByChild, motherByChild, spousesByMember);
        String relationshipBToA = inferRelationship(memberBId, memberAId, userById, parentsByChild, fatherByChild, motherByChild, spousesByMember);
        List<String> commonAncestors = getCommonAncestorNames(memberAId, memberBId, userById, parentsByChild);
        String relationGroup = classifyRelationGroup(relationshipAToB);
        List<String> notes = buildCoverageNotes();

        return RelationshipCompareResponse.builder()
                .memberAId(memberAId)
                .memberBId(memberBId)
                .relationship(relationshipAToB)
                .relationshipFromAToB(relationshipAToB)
                .relationshipFromBToA(relationshipBToA)
                .relationGroup(relationGroup)
                .commonAncestors(commonAncestors)
                .notes(notes)
                .build();
    }

    private String inferRelationship(
            String aId,
            String bId,
            Map<String, User> userById,
            Map<String, Set<String>> parentsByChild,
            Map<String, String> fatherByChild,
            Map<String, String> motherByChild,
            Map<String, Set<String>> spousesByMember
    ) {
        User a = userById.get(aId);
        User b = userById.get(bId);
        if (a == null || b == null) return "Không xác định";

        if (spousesByMember.getOrDefault(aId, Set.of()).contains(bId)) {
            return "Vợ chồng";
        }
        if (parentsByChild.getOrDefault(bId, Set.of()).contains(aId)) {
            return "Cha/mẹ - con";
        }
        if (parentsByChild.getOrDefault(aId, Set.of()).contains(bId)) {
            return "Con - cha/mẹ";
        }

        String directInLaw = directInLawLabel(aId, bId, userById, parentsByChild, spousesByMember);
        if (directInLaw != null) {
            return directInLaw;
        }

        String fullSiblingLabel = siblingLabel(aId, bId, userById, fatherByChild, motherByChild);
        if (fullSiblingLabel != null) {
            return fullSiblingLabel;
        }

        if (isSiblingInLaw(aId, bId, parentsByChild, spousesByMember)
                || isSiblingInLaw(bId, aId, parentsByChild, spousesByMember)) {
            return siblingInLawLabel(a, b);
        }

        Map<String, Integer> aAnc = collectAncestorDistance(aId, parentsByChild, 8);
        Map<String, Integer> bAnc = collectAncestorDistance(bId, parentsByChild, 8);

        Integer aToB = aAnc.get(bId); // b la to tien cua a
        Integer bToA = bAnc.get(aId); // a la to tien cua b
        if (bToA != null) {
            return ancestorLabel(a, b, bToA, aId, bId, fatherByChild, motherByChild);
        }
        if (aToB != null) {
            return descendantLabel(a, b, aToB);
        }

        String uncleAunt = uncleAuntLabel(aId, bId, userById, fatherByChild, motherByChild, parentsByChild);
        if (uncleAunt != null) {
            return uncleAunt;
        }

        if (hasCommonAncestorWithinDepth(aId, bId, parentsByChild, 6)) {
            return cousinLabel(a, b);
        }
        return "Họ hàng";
    }

    private String siblingLabel(
            String aId,
            String bId,
            Map<String, User> userById,
            Map<String, String> fatherByChild,
            Map<String, String> motherByChild
    ) {
        String af = fatherByChild.get(aId);
        String am = motherByChild.get(aId);
        String bf = fatherByChild.get(bId);
        String bm = motherByChild.get(bId);
        boolean sameFather = af != null && af.equals(bf);
        boolean sameMother = am != null && am.equals(bm);
        User a = userById.get(aId);
        User b = userById.get(bId);
        if (a == null || b == null) return null;

        if (sameFather && sameMother && af != null && am != null) {
            return bloodSiblingLabel(a, b);
        }
        if (sameFather && !sameMother) {
            return isOlder(a, b) ? "Anh/chị cùng cha khác mẹ" : "Em cùng cha khác mẹ";
        }
        if (sameMother && !sameFather) {
            return isOlder(a, b) ? "Anh/chị cùng mẹ khác cha" : "Em cùng mẹ khác cha";
        }
        return null;
    }

    private boolean isSiblingInLaw(
            String candidateId,
            String targetId,
            Map<String, Set<String>> parentsByChild,
            Map<String, Set<String>> spousesByMember
    ) {
        Set<String> candidateSpouses = spousesByMember.getOrDefault(candidateId, Set.of());
        for (String spouseId : candidateSpouses) {
            if (areSiblings(spouseId, targetId, parentsByChild)) {
                return true;
            }
        }
        return false;
    }

    private boolean areSiblings(String aId, String bId, Map<String, Set<String>> parentsByChild) {
        Set<String> aParents = parentsByChild.getOrDefault(aId, Set.of());
        Set<String> bParents = parentsByChild.getOrDefault(bId, Set.of());
        return aParents.stream().anyMatch(bParents::contains);
    }

    private String siblingInLawLabel(User a, User b) {
        String ga = a.getGender() == null ? "" : a.getGender().toUpperCase();
        String gb = b.getGender() == null ? "" : b.getGender().toUpperCase();
        if ("FEMALE".equals(ga) && "FEMALE".equals(gb)) {
            return femaleOlderYoungerLabel(a, b, "Chị dâu", "Em dâu");
        }
        if ("MALE".equals(ga) && "MALE".equals(gb)) {
            return maleOlderYoungerLabel(a, b, "Anh rể", "Em rể");
        }
        if ("MALE".equals(ga) && "FEMALE".equals(gb)) {
            return "Anh/em rể - chị/em dâu";
        }
        if ("FEMALE".equals(ga) && "MALE".equals(gb)) {
            return "Chị/em dâu - anh/em rể";
        }
        return "Thông gia";
    }

    private String bloodSiblingLabel(User a, User b) {
        String ga = upper(a.getGender());
        String gb = upper(b.getGender());
        if ("MALE".equals(ga) && "MALE".equals(gb)) {
            return maleOlderYoungerLabel(a, b, "Anh ruột", "Em ruột");
        }
        if ("FEMALE".equals(ga) && "FEMALE".equals(gb)) {
            return femaleOlderYoungerLabel(a, b, "Chị ruột", "Em ruột");
        }
        if ("MALE".equals(ga) && "FEMALE".equals(gb)) {
            return isOlder(a, b) ? "Anh ruột - Em gái ruột" : "Em trai ruột - Chị ruột";
        }
        if ("FEMALE".equals(ga) && "MALE".equals(gb)) {
            return isOlder(a, b) ? "Chị ruột - Em trai ruột" : "Em gái ruột - Anh ruột";
        }
        return "Anh chị em ruột";
    }

    private String cousinLabel(User a, User b) {
        String ga = upper(a.getGender());
        if ("MALE".equals(ga)) {
            return isOlder(a, b) ? "Anh họ" : "Em họ";
        }
        if ("FEMALE".equals(ga)) {
            return isOlder(a, b) ? "Chị họ" : "Em họ";
        }
        return "Anh em họ";
    }

    private String ancestorLabel(
            User ancestor,
            User descendant,
            int distance,
            String ancestorId,
            String descendantId,
            Map<String, String> fatherByChild,
            Map<String, String> motherByChild
    ) {
        String g = upper(ancestor.getGender());
        if (distance == 2) {
            String side = lineageSide(ancestorId, descendantId, fatherByChild, motherByChild);
            if ("NỘI".equals(side)) {
                return "MALE".equals(g) ? "Ông nội" : ("FEMALE".equals(g) ? "Bà nội" : "Ông/bà nội");
            }
            if ("NGOẠI".equals(side)) {
                return "MALE".equals(g) ? "Ông ngoại" : ("FEMALE".equals(g) ? "Bà ngoại" : "Ông/bà ngoại");
            }
            return "MALE".equals(g) ? "Ông" : ("FEMALE".equals(g) ? "Bà" : "Ông/bà");
        }
        if (distance == 3) {
            return "Cụ";
        }
        if (distance == 4) {
            return "Kị";
        }
        return "Tổ tiên";
    }

    private String descendantLabel(User descendant, User ancestor, int distance) {
        if (distance == 2) {
            return "Cháu";
        }
        if (distance == 3) {
            return "Chắt";
        }
        if (distance == 4) {
            return "Chít";
        }
        return "Hậu duệ";
    }

    private String maleOlderYoungerLabel(User a, User b, String olderLabel, String youngerLabel) {
        return isOlder(a, b) ? olderLabel : youngerLabel;
    }

    private String femaleOlderYoungerLabel(User a, User b, String olderLabel, String youngerLabel) {
        return isOlder(a, b) ? olderLabel : youngerLabel;
    }

    private boolean isOlder(User a, User b) {
        if (a.getDob() != null && b.getDob() != null) {
            return a.getDob().isBefore(b.getDob());
        }
        Integer ao = a.getOrderInFamily();
        Integer bo = b.getOrderInFamily();
        if (ao != null && bo != null) {
            return ao < bo;
        }
        return (a.getUserId() != null ? a.getUserId() : "").compareTo(b.getUserId() != null ? b.getUserId() : "") <= 0;
    }

    private String upper(String s) {
        return s == null ? "" : s.toUpperCase();
    }

    private String uncleAuntLabel(
            String candidateId,
            String targetId,
            Map<String, User> userById,
            Map<String, String> fatherByChild,
            Map<String, String> motherByChild,
            Map<String, Set<String>> parentsByChild
    ) {
        String fatherId = fatherByChild.get(targetId);
        String motherId = motherByChild.get(targetId);
        User candidate = userById.get(candidateId);
        if (candidate == null) return null;
        String g = upper(candidate.getGender());

        if (fatherId != null && areSiblings(candidateId, fatherId, parentsByChild)) {
            User father = userById.get(fatherId);
            if ("FEMALE".equals(g)) return "Cô";
            if ("MALE".equals(g)) {
                return (father != null && isOlder(candidate, father)) ? "Bác" : "Chú";
            }
            return "Bác/chú/cô";
        }
        if (motherId != null && areSiblings(candidateId, motherId, parentsByChild)) {
            if ("FEMALE".equals(g)) return "Dì";
            if ("MALE".equals(g)) return "Cậu";
            return "Cậu/dì";
        }
        return null;
    }

    private String lineageSide(
            String ancestorId,
            String descendantId,
            Map<String, String> fatherByChild,
            Map<String, String> motherByChild
    ) {
        return lineageSideDfs(ancestorId, descendantId, fatherByChild, motherByChild, null, 0);
    }

    private String lineageSideDfs(
            String ancestorId,
            String currentId,
            Map<String, String> fatherByChild,
            Map<String, String> motherByChild,
            String firstHop,
            int depth
    ) {
        if (depth > 8 || currentId == null) return null;
        if (ancestorId.equals(currentId)) return firstHop;

        String father = fatherByChild.get(currentId);
        String mother = motherByChild.get(currentId);
        String viaFather = lineageSideDfs(ancestorId, father, fatherByChild, motherByChild, firstHop != null ? firstHop : "NỘI", depth + 1);
        if (viaFather != null) return viaFather;
        return lineageSideDfs(ancestorId, mother, fatherByChild, motherByChild, firstHop != null ? firstHop : "NGOẠI", depth + 1);
    }

    private String classifyRelationGroup(String relation) {
        if (relation == null) return "KHÁC";
        if (relation.contains("Vợ chồng") || relation.contains("rể") || relation.contains("dâu") || relation.contains("Thông gia")) return "THÔNG_GIA";
        if (relation.contains("Ông") || relation.contains("Bà") || relation.contains("Cụ") || relation.contains("Kị") || relation.contains("Cháu") || relation.contains("Chắt") || relation.contains("Chít") || relation.contains("Tổ tiên")) return "TRỰC_HỆ";
        if (relation.contains("Anh") || relation.contains("Chị") || relation.contains("Em") || relation.contains("Bác") || relation.contains("Chú") || relation.contains("Cô") || relation.contains("Cậu") || relation.contains("Dì")) return "BÀNG_HỆ";
        return "KHÁC";
    }

    private List<String> buildCoverageNotes() {
        List<String> notes = new ArrayList<>();
        notes.add("Đã hỗ trợ: trực hệ (ông/bà/cụ/kị - cháu/chắt/chít), nội/ngoại, anh/chị/em ruột và cùng cha/mẹ khác cha/mẹ, bác/chú/cô/cậu/dì, anh/chị/em họ, rể/dâu cơ bản.");
        notes.add("Cần mở rộng schema để đạt đầy đủ: con nuôi, cha dượng/mẹ kế, anh chị em nuôi, con riêng, dâu trưởng/rể trưởng, trưởng họ/trưởng chi/vai vế.");
        return notes;
    }

    private String directInLawLabel(
            String aId,
            String bId,
            Map<String, User> userById,
            Map<String, Set<String>> parentsByChild,
            Map<String, Set<String>> spousesByMember
    ) {
        User a = userById.get(aId);
        User b = userById.get(bId);
        if (a == null || b == null) return null;

        // A la cha/me cua vo/chong cua B  -> Bo/Me chong/vo - Con dau/re
        for (String spouseOfB : spousesByMember.getOrDefault(bId, Set.of())) {
            if (parentsByChild.getOrDefault(spouseOfB, Set.of()).contains(aId)) {
                User spouse = userById.get(spouseOfB);
                return parentInLawPairLabel(a, spouse);
            }
        }

        // B la cha/me cua vo/chong cua A -> Con dau/re - Bo/Me chong/vo
        for (String spouseOfA : spousesByMember.getOrDefault(aId, Set.of())) {
            if (parentsByChild.getOrDefault(spouseOfA, Set.of()).contains(bId)) {
                User spouse = userById.get(spouseOfA);
                return childInLawPairLabel(a, b, spouse);
            }
        }
        return null;
    }

    private String parentInLawPairLabel(User parentInLaw, User childSpouse) {
        String gp = upper(parentInLaw.getGender());
        String gs = upper(childSpouse != null ? childSpouse.getGender() : null);
        boolean spouseIsMale = "MALE".equals(gs);
        String head;
        if ("MALE".equals(gp)) {
            head = spouseIsMale ? "Bố chồng" : "Bố vợ";
        } else if ("FEMALE".equals(gp)) {
            head = spouseIsMale ? "Mẹ chồng" : "Mẹ vợ";
        } else {
            head = spouseIsMale ? "Cha/mẹ chồng" : "Cha/mẹ vợ";
        }
        String tail = spouseIsMale ? "con dâu" : "con rể";
        return head + " - " + tail;
    }

    private String childInLawPairLabel(User childInLaw, User parentInLaw, User ownSpouse) {
        String gs = upper(ownSpouse != null ? ownSpouse.getGender() : null);
        String gc = upper(childInLaw.getGender());
        boolean spouseIsMale = "MALE".equals(gs);
        String head;
        if ("FEMALE".equals(gc) && spouseIsMale) {
            head = "Con dâu";
        } else if ("MALE".equals(gc) && !spouseIsMale) {
            head = "Con rể";
        } else if ("FEMALE".equals(gc)) {
            head = "Con dâu";
        } else if ("MALE".equals(gc)) {
            head = "Con rể";
        } else {
            head = "Con dâu/rể";
        }

        String gp = upper(parentInLaw.getGender());
        String tail;
        if ("MALE".equals(gp)) {
            tail = spouseIsMale ? "bố chồng" : "bố vợ";
        } else if ("FEMALE".equals(gp)) {
            tail = spouseIsMale ? "mẹ chồng" : "mẹ vợ";
        } else {
            tail = spouseIsMale ? "cha/mẹ chồng" : "cha/mẹ vợ";
        }
        return head + " - " + tail;
    }

    private boolean isAncestor(String ancestorId, String memberId, Map<String, Set<String>> parentsByChild, int minDepth) {
        Map<String, Integer> distanceMap = collectAncestorDistance(memberId, parentsByChild, 6);
        Integer distance = distanceMap.get(ancestorId);
        return distance != null && distance >= minDepth;
    }

    private boolean isUncleAuntRelation(String candidateId, String targetId, Map<String, Set<String>> parentsByChild) {
        Set<String> targetParents = parentsByChild.getOrDefault(targetId, Set.of());
        if (targetParents.isEmpty()) return false;
        for (String parentId : targetParents) {
            Set<String> parentParents = parentsByChild.getOrDefault(parentId, Set.of());
            Set<String> candidateParents = parentsByChild.getOrDefault(candidateId, Set.of());
            if (parentParents.stream().anyMatch(candidateParents::contains)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCommonAncestorWithinDepth(String aId, String bId, Map<String, Set<String>> parentsByChild, int maxDepth) {
        Map<String, Integer> aAnc = collectAncestorDistance(aId, parentsByChild, maxDepth);
        Map<String, Integer> bAnc = collectAncestorDistance(bId, parentsByChild, maxDepth);
        for (String anc : aAnc.keySet()) {
            if (bAnc.containsKey(anc)) {
                return true;
            }
        }
        return false;
    }

    private List<String> getCommonAncestorNames(
            String aId,
            String bId,
            Map<String, User> userById,
            Map<String, Set<String>> parentsByChild
    ) {
        Map<String, Integer> aAnc = collectAncestorDistance(aId, parentsByChild, 6);
        Map<String, Integer> bAnc = collectAncestorDistance(bId, parentsByChild, 6);
        Set<String> common = new LinkedHashSet<>();
        for (String anc : aAnc.keySet()) {
            if (bAnc.containsKey(anc)) common.add(anc);
        }
        List<String> names = new ArrayList<>();
        for (String id : common) {
            User u = userById.get(id);
            if (u != null && u.getFullName() != null) {
                names.add(u.getFullName());
            }
        }
        if (names.isEmpty()) {
            names.add("Không có dữ liệu tổ tiên chung");
        }
        return names;
    }

    private Map<String, Integer> collectAncestorDistance(String startId, Map<String, Set<String>> parentsByChild, int maxDepth) {
        Map<String, Integer> dist = new HashMap<>();
        Set<String> frontier = new HashSet<>();
        frontier.add(startId);
        int depth = 0;
        while (!frontier.isEmpty() && depth < maxDepth) {
            depth++;
            Set<String> next = new HashSet<>();
            for (String node : frontier) {
                for (String p : parentsByChild.getOrDefault(node, Set.of())) {
                    dist.putIfAbsent(p, depth);
                    next.add(p);
                }
            }
            frontier = next;
        }
        return dist;
    }

    /** Một {@link Family} và mọi chi con (theo {@code parent_family_id}). */
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