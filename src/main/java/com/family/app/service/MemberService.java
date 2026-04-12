package com.family.app.service;

import com.family.app.dto.ClanMemberStatistics;
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
import java.util.Collection;
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
        return getFamilyTreeData(resolvePublicFamilyTreeRootId(familyId));
    }

    /**
     * Thống kê thành viên trên tập chi đã xác định (vd. cây từ tổ tông) — một nguồn cho /home, /family-tree, dashboard.
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ClanMemberStatistics computeStatisticsForScope(Collection<String> scopeFamilyIds) {
        if (scopeFamilyIds == null || scopeFamilyIds.isEmpty()) {
            return ClanMemberStatistics.empty();
        }
        long total = userRepository.countByFamily_FamilyIdIn(scopeFamilyIds);
        long male = userRepository.countByFamily_FamilyIdInAndGender(scopeFamilyIds, "MALE");
        long female = userRepository.countByFamily_FamilyIdInAndGender(scopeFamilyIds, "FEMALE");
        Integer maxGen = userRepository.findMaxGenerationByFamilyIdIn(scopeFamilyIds);
        int gen = maxGen != null ? maxGen : 0;
        return new ClanMemberStatistics(total, male, female, gen);
    }

    /** Thống kê cho cây gốc {@code rootFamilyId} (gồm mọi chi con). */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ClanMemberStatistics getClanMemberStatistics(String rootFamilyId) {
        familyRepository.findById(rootFamilyId)
                .orElseThrow(() -> new RuntimeException("Dòng họ không tồn tại"));
        Set<String> scopeIds = subtreeFamilyIds(rootFamilyId);
        if (scopeIds.isEmpty()) {
            scopeIds = new LinkedHashSet<>();
            scopeIds.add(rootFamilyId);
        }
        return computeStatisticsForScope(scopeIds);
    }

    /** Gốc cây hiển thị: {@code familyId} nếu có và tồn tại; không thì {@code app.clan.family-id}. */
    private String resolvePublicFamilyTreeRootId(String familyId) {
        String resolved = (familyId != null && !familyId.isBlank())
                ? familyId.trim()
                : (clanProperties.getFamilyId() != null ? clanProperties.getFamilyId().trim() : "");
        if (resolved.isEmpty()) {
            throw new IllegalStateException("Chưa cấu hình dòng họ (app.clan.family-id).");
        }
        familyRepository.findById(resolved)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dòng họ: " + resolved));
        return resolved;
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

        ClanMemberStatistics stats = computeStatisticsForScope(scopeIds);

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

        Map<String, User> userByIdLocal = users.stream().collect(Collectors.toMap(User::getUserId, u -> u));
        for (FamilyTreeResponse.MemberNode node : nodes) {
            User u = userByIdLocal.get(node.getUser_id());
            if (u == null || u.getParentId() == null || u.getParentId().isBlank()) {
                continue;
            }
            User p = userByIdLocal.get(u.getParentId().trim());
            if (p == null) {
                continue;
            }
            if (p.getGender() != null && "MALE".equalsIgnoreCase(p.getGender()) && node.getFatherId() == null) {
                node.setFatherId(p.getUserId());
            } else if (p.getGender() != null && "FEMALE".equalsIgnoreCase(p.getGender()) && node.getMotherId() == null) {
                node.setMotherId(p.getUserId());
            }
        }

        return FamilyTreeResponse.builder()
                .familyName(familyName)
                .totalGenerations(stats.totalGenerations())
                .totalMembers(stats.totalMembers())
                .maleCount(stats.maleCount())
                .femaleCount(stats.femaleCount())
                .members(nodes)
                .build();
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public RelationshipCompareResponse compareRelationship(String familyId, String memberAId, String memberBId) {
        if (memberAId == null || memberBId == null || memberAId.isBlank() || memberBId.isBlank()) {
            throw new IllegalArgumentException("Bắt buộc truyền memberAId/memberBId");
        }

        String clanRoot = resolvePublicFamilyTreeRootId(familyId);
        Set<String> clanScope = subtreeFamilyIds(clanRoot);
        if (clanScope.isEmpty()) {
            clanScope.add(clanRoot);
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
            throw new IllegalArgumentException("Thành viên không thuộc phạm vi dòng họ đang xem.");
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
        mergeParentIdColumnIntoKinshipGraph(userById, parentsByChild, fatherByChild, motherByChild);

        String relationshipAToB = inferRelationship(memberAId, memberBId, userById, parentsByChild, fatherByChild, motherByChild, spousesByMember);
        String relationshipBToA = inferRelationship(memberBId, memberAId, userById, parentsByChild, fatherByChild, motherByChild, spousesByMember);
        relationshipAToB = reconcileCompareForwardKinship(
                relationshipAToB,
                relationshipBToA,
                memberAId,
                memberBId,
                userById,
                parentsByChild,
                fatherByChild,
                motherByChild,
                spousesByMember);
        String relationshipHeadline = preferSpecificCompareHeadline(
                relationshipAToB, relationshipBToA, memberAId, memberBId, userById);
        List<String> commonAncestors = getCommonAncestorNames(memberAId, memberBId, userById, parentsByChild, spousesByMember);
        String relationGroup = classifyRelationGroup(relationshipHeadline);
        List<String> notes = buildCoverageNotes();

        return RelationshipCompareResponse.builder()
                .memberAId(memberAId)
                .memberBId(memberBId)
                .relationship(relationshipHeadline)
                .relationshipFromAToB(relationshipAToB)
                .relationshipFromBToA(relationshipBToA)
                .relationGroup(relationGroup)
                .commonAncestors(commonAncestors)
                .notes(notes)
                .build();
    }

    /**
     * So sánh A rồi B: nếu suy luận A→B rơi vào "Họ hàng" nhưng B→A đã nhận diện được (vd. vợ gọi "Chú chồng"),
     * suy ngược A→B từ quan hệ máu A với chồng của B — đối xứng khi đổi thứ tự chọn.
     */
    private String reconcileCompareForwardKinship(
            String aToB,
            String bToA,
            String aId,
            String bId,
            Map<String, User> userById,
            Map<String, Set<String>> parentsByChild,
            Map<String, String> fatherByChild,
            Map<String, String> motherByChild,
            Map<String, Set<String>> spousesByMember
    ) {
        if (aToB != null && !"Họ hàng".equals(aToB)) {
            return aToB;
        }
        if (bToA == null || "Họ hàng".equals(bToA)) {
            return aToB != null ? aToB : "Họ hàng";
        }
        User ua = userById.get(aId);
        User ub = userById.get(bId);
        if (ua == null || ub == null) {
            return aToB != null ? aToB : "Họ hàng";
        }

        if ("MALE".equalsIgnoreCase(upper(ua.getGender())) && "FEMALE".equalsIgnoreCase(upper(ub.getGender()))) {
            String s = reconcileMaleViewerToWifeWhenSheAddressesHusbandKin(bToA, aId, bId, userById, parentsByChild, fatherByChild, motherByChild, spousesByMember);
            if (s != null) {
                return s;
            }
        }
        if ("FEMALE".equalsIgnoreCase(upper(ua.getGender())) && "MALE".equalsIgnoreCase(upper(ub.getGender()))) {
            String s = reconcileFemaleViewerToHusbandWhenHeAddressesWifeKin(bToA, aId, bId, userById, parentsByChild, fatherByChild, motherByChild, spousesByMember);
            if (s != null) {
                return s;
            }
        }
        return aToB != null ? aToB : "Họ hàng";
    }

    /**
     * Tiêu đề so sánh: nếu A→B chỉ còn “Họ hàng”/“Không xác định” mà B→A đã có nhãn cụ thể thì hiển thị nhãn đó
     * kèm hướng (B → A) để khớp nghĩa; tránh quy hết về họ hàng khi đổi thứ tự chọn.
     */
    private String preferSpecificCompareHeadline(
            String aToB,
            String bToA,
            String aId,
            String bId,
            Map<String, User> userById
    ) {
        if (!isUnspecificKinship(aToB)) {
            return aToB;
        }
        if (isUnspecificKinship(bToA)) {
            return aToB != null ? aToB : "Họ hàng";
        }
        User ua = userById.get(aId);
        User ub = userById.get(bId);
        String an = ua != null && ua.getFullName() != null && !ua.getFullName().isBlank()
                ? ua.getFullName().trim() : "Người thứ nhất";
        String bn = ub != null && ub.getFullName() != null && !ub.getFullName().isBlank()
                ? ub.getFullName().trim() : "Người thứ hai";
        return bToA + " (" + bn + " → " + an + ")";
    }

    private static boolean isUnspecificKinship(String s) {
        return s == null || "Họ hàng".equals(s) || "Không xác định".equals(s);
    }

    /** B (vợ) gọi A (bên máu chồng) kiểu * chồng → A gọi B là con/cháu dâu. */
    private String reconcileMaleViewerToWifeWhenSheAddressesHusbandKin(
            String bToA,
            String maleViewerId,
            String wifeId,
            Map<String, User> userById,
            Map<String, Set<String>> parentsByChild,
            Map<String, String> fatherByChild,
            Map<String, String> motherByChild,
            Map<String, Set<String>> spousesByMember
    ) {
        if (bToA == null || !bToA.contains("chồng")) {
            return null;
        }
        if (bToA.contains("Vợ chồng")) {
            return null;
        }
        if (bToA.contains("Cha chồng") || bToA.contains("Mẹ chồng") || bToA.contains("cha/mẹ chồng")) {
            return null;
        }
        String hId = maleSpouseId(wifeId, spousesByMember, userById);
        if (hId == null || hId.equals(maleViewerId)) {
            return null;
        }
        String maleToHusband = inferRelationship(
                maleViewerId,
                hId,
                userById,
                parentsByChild,
                fatherByChild,
                motherByChild,
                spousesByMember);
        if (maleToHusband == null || "Họ hàng".equals(maleToHusband)) {
            return null;
        }
        return mapHusbandBloodKinshipToHowHeCallsNephewWife(maleToHusband, bToA);
    }

    /** B (chồng) gọi A (bên máu vợ) kiểu * vợ → A gọi B là con/cháu rể (đối xứng). */
    private String reconcileFemaleViewerToHusbandWhenHeAddressesWifeKin(
            String bToA,
            String femaleViewerId,
            String husbandId,
            Map<String, User> userById,
            Map<String, Set<String>> parentsByChild,
            Map<String, String> fatherByChild,
            Map<String, String> motherByChild,
            Map<String, Set<String>> spousesByMember
    ) {
        if (bToA == null || !bToA.contains("vợ")) {
            return null;
        }
        if (bToA.contains("Bố vợ") || bToA.contains("Mẹ vợ") || bToA.contains("cha/mẹ vợ")) {
            return null;
        }
        String wId = femaleSpouseId(husbandId, spousesByMember, userById);
        if (wId == null || wId.equals(femaleViewerId)) {
            return null;
        }
        String femaleToWife = inferRelationship(
                femaleViewerId,
                wId,
                userById,
                parentsByChild,
                fatherByChild,
                motherByChild,
                spousesByMember);
        if (femaleToWife == null || "Họ hàng".equals(femaleToWife)) {
            return null;
        }
        return mapWifeBloodKinshipToHowSheCallsNieceHusband(femaleToWife, bToA);
    }

    private String mapHusbandBloodKinshipToHowHeCallsNephewWife(String maleToHusbandKinship, String wifeToMaleLabel) {
        if (wifeToMaleLabel.contains("Anh chồng")) {
            return "Em dâu";
        }
        if (wifeToMaleLabel.contains("Chị chồng")) {
            return "Em dâu";
        }
        if (wifeToMaleLabel.contains("Em chồng")) {
            return "Chị dâu";
        }
        String k = maleToHusbandKinship;
        if (k.contains("Chú") || k.contains("Bác") || k.contains("Cô") || k.contains("Cậu") || k.contains("Dì")) {
            return "Cháu dâu";
        }
        if (k.contains("ông nội") || k.contains("Bà nội") || k.contains("ông ngoại") || k.contains("Bà ngoại")
                || k.contains("Cụ") || k.contains("Kị") || k.contains("Tổ tiên")) {
            return "Cháu dâu";
        }
        if ("Con".equals(k) || k.startsWith("Con ") || k.contains("Cha/mẹ") || k.contains("Cha ") || k.contains("Mẹ ")) {
            return "Con dâu";
        }
        if (k.contains("Cháu") || k.contains("Chắt") || k.contains("Chít")) {
            return "Cháu dâu";
        }
        if (k.contains("Anh") || k.contains("Chị") || k.contains("Em")) {
            return "Em dâu";
        }
        return "Cháu dâu";
    }

    private String mapWifeBloodKinshipToHowSheCallsNieceHusband(String femaleToWifeKinship, String husbandToFemaleLabel) {
        if (husbandToFemaleLabel.contains("Anh vợ")) {
            return "Em rể";
        }
        if (husbandToFemaleLabel.contains("Chị vợ")) {
            return "Em rể";
        }
        if (husbandToFemaleLabel.contains("Em vợ")) {
            return "Anh rể";
        }
        String k = femaleToWifeKinship;
        if (k.contains("Chú") || k.contains("Bác") || k.contains("Cô") || k.contains("Cậu") || k.contains("Dì")) {
            return "Cháu rể";
        }
        if (k.contains("ông nội") || k.contains("Bà nội") || k.contains("ông ngoại") || k.contains("Bà ngoại")
                || k.contains("Cụ") || k.contains("Kị") || k.contains("Tổ tiên")) {
            return "Cháu rể";
        }
        if ("Con".equals(k) || k.startsWith("Con ") || k.contains("Cha/mẹ") || k.contains("Cha ") || k.contains("Mẹ ")) {
            return "Con rể";
        }
        if (k.contains("Cháu") || k.contains("Chắt") || k.contains("Chít")) {
            return "Cháu rể";
        }
        if (k.contains("Anh") || k.contains("Chị") || k.contains("Em")) {
            return "Em rể";
        }
        return "Cháu rể";
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

        String affine = affineSpouseFamilyKinship(aId, bId, userById, parentsByChild, fatherByChild, motherByChild, spousesByMember);
        if (affine != null) {
            return affine;
        }

        String spouseLineBridge = spouseBloodlineBridgeKinship(aId, bId, userById, parentsByChild, fatherByChild, motherByChild, spousesByMember);
        if (spouseLineBridge != null) {
            return spouseLineBridge;
        }

        String viaOthersSpouse = kinshipViaBloodRelativeToSpouseOfOther(
                aId, bId, userById, parentsByChild, fatherByChild, motherByChild, spousesByMember);
        if (viaOthersSpouse != null) {
            return viaOthersSpouse;
        }

        String twoSpousesLine = twoSpousesOnSameBloodlineKinship(aId, bId, userById, parentsByChild, fatherByChild, motherByChild, spousesByMember);
        if (twoSpousesLine != null) {
            return twoSpousesLine;
        }

        String bloodToSpouseOfDescendant = patrilineOrMatrilineBloodToSpouseOfDescendantKinship(
                aId, bId, userById, parentsByChild, fatherByChild, motherByChild, spousesByMember);
        if (bloodToSpouseOfDescendant != null) {
            return bloodToSpouseOfDescendant;
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

        String collateralAcrossBranches = collateralKinshipAcrossBranchesByGeneration(
                aId, bId, a, b, parentsByChild, 6);
        if (collateralAcrossBranches != null) {
            return collateralAcrossBranches;
        }

        if (hasCommonAncestorWithinDepth(aId, bId, parentsByChild, 6)) {
            return cousinLabel(a, b);
        }
        return "Họ hàng";
    }

    /**
     * Hai nhánh song song có tổ tiên chung nhưng không rơi vào nhận diện chú/bác (chỉ xét cha/mẹ một đời):
     * nếu {@code generation} lệch từ 2 bậc trở lên thì không gọi là anh/chị/em họ — dùng ông/bà/cụ ↔ cháu/chắt/chít
     * theo khoảng thế hệ (vd. em của ông nội ↔ chắt: ông – cháu).
     */
    private String collateralKinshipAcrossBranchesByGeneration(
            String aId,
            String bId,
            User a,
            User b,
            Map<String, Set<String>> parentsByChild,
            int maxDepthForCommonAnc
    ) {
        if (a == null || b == null) {
            return null;
        }
        Integer ga = a.getGeneration();
        Integer gb = b.getGeneration();
        if (ga == null || gb == null) {
            return null;
        }
        if (!hasCommonAncestorWithinDepth(aId, bId, parentsByChild, maxDepthForCommonAnc)) {
            return null;
        }
        int genDiff = gb - ga;
        if (genDiff >= 2) {
            return collateralElderToYoungerByGenerationGap(genDiff);
        }
        if (genDiff <= -2) {
            return collateralYoungerToElderByGenerationGap(b, -genDiff);
        }
        return null;
    }

    /** Người thế hệ nhỏ hơn (số đời lớn hơn) gọi người thế hệ trên — không phân nội/ngoại. */
    private static String collateralElderToYoungerByGenerationGap(int generationGap) {
        return switch (generationGap) {
            case 2 -> "Cháu";
            case 3 -> "Chắt";
            case 4 -> "Chít";
            default -> generationGap > 4 ? "Hậu duệ" : "Cháu";
        };
    }

    /** Người thế hệ sau gọi người thế hệ trước trên nhánh bàng phụ — không phân nội/ngoại. */
    private static String collateralYoungerToElderByGenerationGap(User elder, int generationGap) {
        String g = upper(elder.getGender());
        return switch (generationGap) {
            case 2 -> "MALE".equals(g) ? "Ông" : ("FEMALE".equals(g) ? "Bà" : "Ông/bà");
            case 3 -> "Cụ";
            case 4 -> "Kị";
            default -> generationGap > 4 ? "Tổ tiên" : "Cụ";
        };
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

    private static String upper(String s) {
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
        if (relation.contains("Vợ chồng") || relation.contains("Thông gia")
                || relation.contains("rể") || relation.contains("dâu")
                || relation.contains("chồng") || relation.contains("vợ")) {
            return "THÔNG_GIA";
        }
        if (relation.contains("Ông") || relation.contains("Bà") || relation.contains("Cụ") || relation.contains("Kị") || relation.contains("Cháu") || relation.contains("Chắt") || relation.contains("Chít") || relation.contains("Tổ tiên")
                || "Con".equals(relation) || "Mẹ".equals(relation) || "Cha".equals(relation)) return "TRỰC_HỆ";
        if (relation.contains("Anh") || relation.contains("Chị") || relation.contains("Em") || relation.contains("Bác") || relation.contains("Chú") || relation.contains("Cô") || relation.contains("Cậu") || relation.contains("Dì")) return "BÀNG_HỆ";
        return "KHÁC";
    }

    private List<String> buildCoverageNotes() {
        List<String> notes = new ArrayList<>();
        notes.add("Đã hỗ trợ: trực hệ, nội/ngoại, anh/chị/em ruột/cùng cha mẹ, bác/chú/cô/cậu/dì, họ (cùng đời), ông/bà–cháu giữa nhánh khi lệch ≥2 thế hệ và có tổ tiên chung, thông gia cơ bản.");
        notes.add("Vợ/chồng (chỉ SPOUSE): hậu duệ máu của chồng/vợ được gọi như chồng/vợ (Cháu, Chắt, Chít…); chiều ngược: Bà nội/Cụ… theo nhánh máu chồng (và đối xứng bên vợ).");
        notes.add("Hai nữ (hai dâu) có chồng cùng nhánh máu: Con dâu / Cháu dâu / Chắt dâu / Chít dâu theo bậc chồng; chiều ngược: Mẹ chồng, Bà nội, Cụ… (tương tự hai rể bên vợ).");
        notes.add("Con dâu ↔ nhà chồng: cha/mẹ chồng, anh/chị/em chồng, chị/em dâu & anh/em rể (vợ/chồng của anh chị em chồng), cháu (bác dâu/thím/cô), bác/chú/cô/cậu/dì chồng và vợ/chồng của họ (thím, bác gái, dượng…).");
        notes.add("Con rể ↔ nhà vợ: bố/mẹ vợ, anh/chị/em vợ, cháu & bác/chú/cô bên vợ (tương tự).");
        notes.add("Hạn chế: không phân biệt đích danh mợ/dượng theo vùng; cùng giới vợ chồng / dữ liệu thiếu giới tính có thể gọi chung.");
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
            Map<String, Set<String>> parentsByChild,
            Map<String, Set<String>> spousesByMember
    ) {
        Map<String, Integer> aAnc = collectAncestorDistance(aId, parentsByChild, 12);
        Map<String, Integer> bAnc = collectAncestorDistance(bId, parentsByChild, 12);
        Set<String> common = new LinkedHashSet<>();
        for (String anc : aAnc.keySet()) {
            if (bAnc.containsKey(anc)) {
                common.add(anc);
            }
        }
        List<String> names = new ArrayList<>();
        for (String id : common) {
            User u = userById.get(id);
            if (u != null && u.getFullName() != null) {
                names.add(u.getFullName());
            }
        }
        if (names.isEmpty()) {
            names.addAll(bloodlineBridgeCommonAncestors(aId, bId, userById, parentsByChild, spousesByMember));
        }
        if (names.isEmpty()) {
            names.addAll(sharedBloodAncestorsViaSpouses(aId, bId, userById, parentsByChild, spousesByMember));
        }
        if (names.isEmpty()) {
            names.add("Không có dữ liệu tổ tiên chung");
        }
        return names;
    }

    /**
     * Hai người (thường dâu) không có cạnh cha–con trong đồ thị nhưng chồng cùng nằm trên cây máu:
     * tổ tiên chung = giao tập tổ tiên máu của hai chồng (và đối xứng qua vợ nếu cả hai là nam có vợ).
     */
    private List<String> sharedBloodAncestorsViaSpouses(
            String aId,
            String bId,
            Map<String, User> userById,
            Map<String, Set<String>> parentsByChild,
            Map<String, Set<String>> spousesByMember
    ) {
        List<String> out = new ArrayList<>();
        String h1 = maleSpouseId(aId, spousesByMember, userById);
        String h2 = maleSpouseId(bId, spousesByMember, userById);
        if (h1 != null && h2 != null) {
            appendIntersectingAncestorNames(out, h1, h2, userById, parentsByChild);
        }
        if (out.isEmpty()) {
            String w1 = femaleSpouseId(aId, spousesByMember, userById);
            String w2 = femaleSpouseId(bId, spousesByMember, userById);
            if (w1 != null && w2 != null) {
                appendIntersectingAncestorNames(out, w1, w2, userById, parentsByChild);
            }
        }
        if (out.isEmpty()) {
            User ua = userById.get(aId);
            User ub = userById.get(bId);
            if (ua != null && ub != null) {
                if ("MALE".equalsIgnoreCase(upper(ua.getGender()))
                        && "FEMALE".equalsIgnoreCase(upper(ub.getGender()))) {
                    String hB = maleSpouseId(bId, spousesByMember, userById);
                    if (hB != null) {
                        appendIntersectingAncestorNames(out, aId, hB, userById, parentsByChild);
                    }
                } else if ("FEMALE".equalsIgnoreCase(upper(ua.getGender()))
                        && "MALE".equalsIgnoreCase(upper(ub.getGender()))) {
                    String hA = maleSpouseId(aId, spousesByMember, userById);
                    if (hA != null) {
                        appendIntersectingAncestorNames(out, hA, bId, userById, parentsByChild);
                    }
                }
            }
        }
        return out;
    }

    private void appendIntersectingAncestorNames(
            List<String> out,
            String bloodId1,
            String bloodId2,
            Map<String, User> userById,
            Map<String, Set<String>> parentsByChild
    ) {
        Map<String, Integer> d1 = collectAncestorDistance(bloodId1, parentsByChild, 16);
        Map<String, Integer> d2 = collectAncestorDistance(bloodId2, parentsByChild, 16);
        Set<String> seen = new LinkedHashSet<>();
        for (String id : d1.keySet()) {
            if (!d2.containsKey(id)) {
                continue;
            }
            User u = userById.get(id);
            if (u == null || u.getFullName() == null || u.getFullName().isBlank()) {
                continue;
            }
            String nm = u.getFullName().trim();
            if (seen.add(nm)) {
                out.add(nm);
            }
        }
    }

    /**
     * Khi một người chỉ nối với cây qua vợ/chồng (SPOUSE), tổ tiên máu không giao — hiển thị mối nối qua chồng/vợ + tổ tiên chung trên nhánh máu đó.
     */
    private List<String> bloodlineBridgeCommonAncestors(
            String aId,
            String bId,
            Map<String, User> userById,
            Map<String, Set<String>> parentsByChild,
            Map<String, Set<String>> spousesByMember
    ) {
        List<String> out = new ArrayList<>();
        User ua = userById.get(aId);
        User ub = userById.get(bId);
        if (ua == null || ub == null) {
            return out;
        }
        if ("FEMALE".equalsIgnoreCase(upper(ua.getGender()))) {
            String hId = maleSpouseId(aId, spousesByMember, userById);
            if (hId != null && collectAncestorDistance(bId, parentsByChild, 12).containsKey(hId)) {
                appendBloodlineBridgeNames(out, hId, bId, userById, parentsByChild, "chồng — mối nối nhánh máu");
            }
        }
        if (out.isEmpty() && "FEMALE".equalsIgnoreCase(upper(ub.getGender()))) {
            String hId = maleSpouseId(bId, spousesByMember, userById);
            if (hId != null && collectAncestorDistance(aId, parentsByChild, 12).containsKey(hId)) {
                appendBloodlineBridgeNames(out, hId, aId, userById, parentsByChild, "chồng — mối nối nhánh máu");
            }
        }
        if (out.isEmpty() && "MALE".equalsIgnoreCase(upper(ua.getGender()))) {
            String wId = femaleSpouseId(aId, spousesByMember, userById);
            if (wId != null && collectAncestorDistance(bId, parentsByChild, 12).containsKey(wId)) {
                appendBloodlineBridgeNames(out, wId, bId, userById, parentsByChild, "vợ — mối nối nhánh máu");
            }
        }
        if (out.isEmpty() && "MALE".equalsIgnoreCase(upper(ub.getGender()))) {
            String wId = femaleSpouseId(bId, spousesByMember, userById);
            if (wId != null && collectAncestorDistance(aId, parentsByChild, 12).containsKey(wId)) {
                appendBloodlineBridgeNames(out, wId, aId, userById, parentsByChild, "vợ — mối nối nhánh máu");
            }
        }
        if (out.isEmpty()
                && "FEMALE".equalsIgnoreCase(upper(ua.getGender()))
                && "FEMALE".equalsIgnoreCase(upper(ub.getGender()))) {
            String h1 = maleSpouseId(aId, spousesByMember, userById);
            String h2 = maleSpouseId(bId, spousesByMember, userById);
            if (h1 != null && h2 != null) {
                if (collectAncestorDistance(h2, parentsByChild, 12).containsKey(h1)) {
                    appendBloodlineBridgeNames(out, h1, h2, userById, parentsByChild, "cùng nhánh chồng (hai dâu)");
                } else if (collectAncestorDistance(h1, parentsByChild, 12).containsKey(h2)) {
                    appendBloodlineBridgeNames(out, h2, h1, userById, parentsByChild, "cùng nhánh chồng (hai dâu)");
                }
            }
        }
        if (out.isEmpty()
                && "MALE".equalsIgnoreCase(upper(ua.getGender()))
                && "MALE".equalsIgnoreCase(upper(ub.getGender()))) {
            String w1 = femaleSpouseId(aId, spousesByMember, userById);
            String w2 = femaleSpouseId(bId, spousesByMember, userById);
            if (w1 != null && w2 != null) {
                if (collectAncestorDistance(w2, parentsByChild, 12).containsKey(w1)) {
                    appendBloodlineBridgeNames(out, w1, w2, userById, parentsByChild, "cùng nhánh vợ (hai rể)");
                } else if (collectAncestorDistance(w1, parentsByChild, 12).containsKey(w2)) {
                    appendBloodlineBridgeNames(out, w2, w1, userById, parentsByChild, "cùng nhánh vợ (hai rể)");
                }
            }
        }
        if (out.isEmpty()
                && "MALE".equalsIgnoreCase(upper(ua.getGender()))
                && "FEMALE".equalsIgnoreCase(upper(ub.getGender()))) {
            String hB = maleSpouseId(bId, spousesByMember, userById);
            if (hB != null && collectAncestorDistance(hB, parentsByChild, 12).containsKey(aId)) {
                appendBloodlineBridgeNames(out, aId, hB, userById, parentsByChild, "nhánh máu — dâu (ông & cháu dâu)");
            }
        }
        if (out.isEmpty()
                && "FEMALE".equalsIgnoreCase(upper(ua.getGender()))
                && "MALE".equalsIgnoreCase(upper(ub.getGender()))) {
            String hA = maleSpouseId(aId, spousesByMember, userById);
            if (hA != null && collectAncestorDistance(hA, parentsByChild, 12).containsKey(bId)) {
                appendBloodlineBridgeNames(out, bId, hA, userById, parentsByChild, "nhánh máu — dâu (ông & cháu dâu)");
            }
        }
        return out;
    }

    private void appendBloodlineBridgeNames(
            List<String> out,
            String bloodAnchorId,
            String descendantId,
            Map<String, User> userById,
            Map<String, Set<String>> parentsByChild,
            String bridgeNote
    ) {
        User anchor = userById.get(bloodAnchorId);
        if (anchor != null && anchor.getFullName() != null) {
            out.add(anchor.getFullName() + " (" + bridgeNote + ")");
        }
        Map<String, Integer> anchorAnc = collectAncestorDistance(bloodAnchorId, parentsByChild, 12);
        Map<String, Integer> descAnc = collectAncestorDistance(descendantId, parentsByChild, 12);
        for (String anc : anchorAnc.keySet()) {
            if (descAnc.containsKey(anc) && !anc.equals(bloodAnchorId)) {
                User u = userById.get(anc);
                if (u != null && u.getFullName() != null) {
                    out.add(u.getFullName());
                }
            }
        }
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

    /**
     * Nam so với nữ chỉ nối cây qua chồng (dâu), hoặc hai nữ — đối phương là vợ của người nam trên nhánh máu:
     * suy luận quan hệ máu người xem ↔ chồng của đối phương (chú/bác/ông…), rồi ánh xạ sang cách gọi vợ (thím/mợ/bà…).
     * Bổ sung cho {@link #spouseBloodlineBridgeKinship} — chỉ bắt tổ tiên thẳng trên chuỗi cha–con, không bắt bàng hệ.
     */
    private String kinshipViaBloodRelativeToSpouseOfOther(
            String viewerId,
            String otherId,
            Map<String, User> userById,
            Map<String, Set<String>> parentsByChild,
            Map<String, String> fatherByChild,
            Map<String, String> motherByChild,
            Map<String, Set<String>> spousesByMember
    ) {
        User viewer = userById.get(viewerId);
        User other = userById.get(otherId);
        if (viewer == null || other == null) {
            return null;
        }

        if ("MALE".equalsIgnoreCase(upper(viewer.getGender()))
                && "FEMALE".equalsIgnoreCase(upper(other.getGender()))) {
            String hId = maleSpouseId(otherId, spousesByMember, userById);
            if (hId == null || hId.equals(viewerId)) {
                return null;
            }
            String kin = inferRelationship(
                    viewerId,
                    hId,
                    userById,
                    parentsByChild,
                    fatherByChild,
                    motherByChild,
                    spousesByMember);
            return mapMaleViewerKinshipToWifeOfMaleBloodKin(kin);
        }

        if ("FEMALE".equalsIgnoreCase(upper(viewer.getGender()))
                && "FEMALE".equalsIgnoreCase(upper(other.getGender()))) {
            String hOther = maleSpouseId(otherId, spousesByMember, userById);
            if (hOther == null || hOther.equals(viewerId)) {
                return null;
            }
            String kin = inferRelationship(
                    viewerId,
                    hOther,
                    userById,
                    parentsByChild,
                    fatherByChild,
                    motherByChild,
                    spousesByMember);
            return mapFemaleViewerKinshipToWifeOfMaleBloodKin(kin);
        }

        if ("FEMALE".equalsIgnoreCase(upper(viewer.getGender()))
                && "MALE".equalsIgnoreCase(upper(other.getGender()))) {
            String hViewer = maleSpouseId(viewerId, spousesByMember, userById);
            if (hViewer == null || hViewer.equals(otherId)) {
                return null;
            }
            String kin = inferRelationship(
                    hViewer,
                    otherId,
                    userById,
                    parentsByChild,
                    fatherByChild,
                    motherByChild,
                    spousesByMember);
            return mapWifeViewerThroughHusbandToMaleKin(kin);
        }

        return null;
    }

    /**
     * Nữ so với nam trên nhánh máu: lấy quan hệ chồng mình ↔ đối phương, thêm ngữ cảnh nhà chồng khi cần
     * (Chú → Chú chồng), còn lại giữ nhãn đã suy (anh họ, ông nội…).
     */
    private String mapWifeViewerThroughHusbandToMaleKin(String kin) {
        if (kin == null || "Họ hàng".equals(kin) || "Không xác định".equals(kin)) {
            return null;
        }
        if (kin.contains("Vợ chồng")) {
            return null;
        }
        if (kin.contains("chồng") || kin.contains("vợ") || kin.contains("dâu") || kin.contains("rể")) {
            return kin;
        }
        if (kin.contains("Chú")) {
            return "Chú chồng";
        }
        if (kin.contains("Bác")) {
            return "Bác chồng";
        }
        if (kin.contains("Cậu")) {
            return "Cậu chồng";
        }
        if (kin.contains("Dì")) {
            return "Dì chồng";
        }
        if (kin.contains("Cô")) {
            return "Cô chồng";
        }
        return kin;
    }

    /** Nam gọi vợ của người nam có quan hệ máu {@code kin} với mình (vd. Ông → Bà, Chú → Thím). */
    private String mapMaleViewerKinshipToWifeOfMaleBloodKin(String kin) {
        if (kin == null || "Họ hàng".equals(kin) || "Không xác định".equals(kin)) {
            return null;
        }
        if (kin.contains("Vợ chồng")) {
            return null;
        }
        if (kin.contains("Chú chồng")) {
            return "Thím chồng";
        }
        if (kin.contains("Chú")) {
            return "Thím";
        }
        if (kin.contains("Bác")) {
            return "Mợ";
        }
        if (kin.contains("Cậu")) {
            return "Mợ";
        }
        if (kin.contains("Ông nội") || kin.contains("ông nội")) {
            return "Bà nội";
        }
        if (kin.contains("Ông ngoại") || kin.contains("ông ngoại")) {
            return "Bà ngoại";
        }
        if (kin.contains("Bà nội") || kin.contains("bà nội")) {
            return "Ông nội";
        }
        if (kin.contains("Bà ngoại") || kin.contains("bà ngoại")) {
            return "Ông ngoại";
        }
        if (kin.contains("Ông") || kin.contains("Cụ") || kin.contains("Kị") || kin.contains("Tổ tiên")) {
            return "Bà";
        }
        if (kin.startsWith("Bà") || kin.equals("Bà")) {
            return "Ông";
        }
        return mapHusbandBloodKinshipToHowHeCallsNephewWife(kin, "");
    }

    /** Nữ gọi vợ của người nam có quan hệ {@code kin} với mình (thường qua nhà chồng: Chú chồng → Thím chồng). */
    private String mapFemaleViewerKinshipToWifeOfMaleBloodKin(String kin) {
        if (kin == null || "Họ hàng".equals(kin) || "Không xác định".equals(kin)) {
            return null;
        }
        if (kin.contains("Vợ chồng")) {
            return null;
        }
        if (kin.contains("Chú chồng")) {
            return "Thím chồng";
        }
        if (kin.contains("Bác chồng")) {
            return "Mợ chồng";
        }
        if (kin.contains("Cậu chồng")) {
            return "Mợ chồng";
        }
        if (kin.contains("Anh chồng")) {
            return "Chị dâu";
        }
        if (kin.contains("Chị chồng")) {
            return "Chị dâu";
        }
        if (kin.contains("Em chồng")) {
            return "Em dâu";
        }
        if (kin.contains("Ông nội") || kin.contains("ông nội")) {
            return "Bà nội";
        }
        if (kin.contains("Ông ngoại") || kin.contains("ông ngoại")) {
            return "Bà ngoại";
        }
        if (kin.contains("Bà nội") || kin.contains("bà nội")) {
            return "Ông nội";
        }
        if (kin.contains("Bà ngoại") || kin.contains("bà ngoại")) {
            return "Ông ngoại";
        }
        if (kin.contains("Chú")) {
            return "Thím";
        }
        if (kin.contains("Bác")) {
            return "Mợ";
        }
        if (kin.contains("Cậu")) {
            return "Mợ";
        }
        if (kin.contains("Ông") || kin.contains("Cụ") || kin.contains("Kị") || kin.contains("Tổ tiên")) {
            return "Bà";
        }
        if (kin.startsWith("Bà") || kin.equals("Bà")) {
            return "Ông";
        }
        return mapHusbandBloodKinshipToHowHeCallsNephewWife(kin, "");
    }

    /**
     * Vợ/chồng chỉ có cạnh SPOUSE với người mang dòng máu: hậu duệ của chồng/vợ được gọi như chồng/vợ gọi (Cháu, Chắt, Chít…);
     * chiều ngược: Bà nội / Cụ / Kị… theo nhánh máu của chồng (hoặc tương tự bên vợ).
     */
    private String spouseBloodlineBridgeKinship(
            String viewerId,
            String otherId,
            Map<String, User> userById,
            Map<String, Set<String>> parentsByChild,
            Map<String, String> fatherByChild,
            Map<String, String> motherByChild,
            Map<String, Set<String>> spousesByMember
    ) {
        User viewer = userById.get(viewerId);
        User other = userById.get(otherId);
        if (viewer == null || other == null) {
            return null;
        }

        if ("FEMALE".equalsIgnoreCase(upper(viewer.getGender()))) {
            String hId = maleSpouseId(viewerId, spousesByMember, userById);
            if (hId != null && !hId.equals(otherId)) {
                Integer d = collectAncestorDistance(otherId, parentsByChild, 24).get(hId);
                if (d != null && d >= 1) {
                    if (d == 1) {
                        return "Con";
                    }
                    return descendantLabel(other, viewer, d);
                }
            }
        }

        if ("FEMALE".equalsIgnoreCase(upper(other.getGender()))) {
            String hId = maleSpouseId(otherId, spousesByMember, userById);
            if (hId != null && !hId.equals(viewerId)) {
                Integer d = collectAncestorDistance(viewerId, parentsByChild, 24).get(hId);
                if (d != null && d >= 1) {
                    if (d == 1) {
                        return "Mẹ";
                    }
                    String side = lineageSide(hId, viewerId, fatherByChild, motherByChild);
                    return ancestorLabelAffine(other, viewer, d, side);
                }
            }
        }

        if ("MALE".equalsIgnoreCase(upper(viewer.getGender()))) {
            String wId = femaleSpouseId(viewerId, spousesByMember, userById);
            if (wId != null && !wId.equals(otherId)) {
                Integer d = collectAncestorDistance(otherId, parentsByChild, 24).get(wId);
                if (d != null && d >= 1) {
                    if (d == 1) {
                        return "Con";
                    }
                    return descendantLabel(other, viewer, d);
                }
            }
        }

        if ("MALE".equalsIgnoreCase(upper(other.getGender()))) {
            String wId = femaleSpouseId(otherId, spousesByMember, userById);
            if (wId != null && !wId.equals(viewerId)) {
                Integer d = collectAncestorDistance(viewerId, parentsByChild, 24).get(wId);
                if (d != null && d >= 1) {
                    if (d == 1) {
                        return "Cha";
                    }
                    String side = lineageSide(wId, viewerId, fatherByChild, motherByChild);
                    return ancestorLabelAffine(other, viewer, d, side);
                }
            }
        }

        return null;
    }

    /** Giống {@link #ancestorLabel} nhưng nội/ngoại tính theo mốc máu {@code lineageAnchorId} (chồng/vợ), không theo bản thân người được gọi. */
    private String ancestorLabelAffine(User ancestor, User descendant, int distance, String side) {
        String g = upper(ancestor.getGender());
        if (distance == 2) {
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

    /**
     * Hai người đều là vợ/chồng của các thành viên máu cùng nhánh (vd. bà Huệ — vợ Thịnh, Vân — vợ cháu nội Thắng):
     * khoảng cách thế hệ giữa hai người chồng/vợ máu → cháu dâu / chắt dâu / chít dâu (hoặc cháu rể…).
     */
    private String twoSpousesOnSameBloodlineKinship(
            String viewerId,
            String otherId,
            Map<String, User> userById,
            Map<String, Set<String>> parentsByChild,
            Map<String, String> fatherByChild,
            Map<String, String> motherByChild,
            Map<String, Set<String>> spousesByMember
    ) {
        User viewer = userById.get(viewerId);
        User other = userById.get(otherId);
        if (viewer == null || other == null) {
            return null;
        }

        if ("FEMALE".equalsIgnoreCase(upper(viewer.getGender())) && "FEMALE".equalsIgnoreCase(upper(other.getGender()))) {
            String hViewer = maleSpouseId(viewerId, spousesByMember, userById);
            String hOther = maleSpouseId(otherId, spousesByMember, userById);
            if (hViewer != null && hOther != null && !hViewer.equals(hOther)) {
                Integer dOtherUpToViewerHusband = collectAncestorDistance(hOther, parentsByChild, 24).get(hViewer);
                if (dOtherUpToViewerHusband != null && dOtherUpToViewerHusband >= 1) {
                    return wifeOfElderPatrilineToYoungerWifeForward(dOtherUpToViewerHusband);
                }
                Integer dViewerUpToOtherHusband = collectAncestorDistance(hViewer, parentsByChild, 24).get(hOther);
                if (dViewerUpToOtherHusband != null && dViewerUpToOtherHusband >= 1) {
                    String side = lineageSide(hOther, hViewer, fatherByChild, motherByChild);
                    return wifeOfElderPatrilineToYoungerWifeReverse(other, viewer, dViewerUpToOtherHusband, side);
                }
            }
        }

        if ("MALE".equalsIgnoreCase(upper(viewer.getGender())) && "MALE".equalsIgnoreCase(upper(other.getGender()))) {
            String wViewer = femaleSpouseId(viewerId, spousesByMember, userById);
            String wOther = femaleSpouseId(otherId, spousesByMember, userById);
            if (wViewer != null && wOther != null && !wViewer.equals(wOther)) {
                Integer dOtherUpToViewerWife = collectAncestorDistance(wOther, parentsByChild, 24).get(wViewer);
                if (dOtherUpToViewerWife != null && dOtherUpToViewerWife >= 1) {
                    return husbandOfElderMatrilineToYoungerHusbandForward(dOtherUpToViewerWife);
                }
                Integer dViewerUpToOtherWife = collectAncestorDistance(wViewer, parentsByChild, 24).get(wOther);
                if (dViewerUpToOtherWife != null && dViewerUpToOtherWife >= 1) {
                    String side = lineageSide(wOther, wViewer, fatherByChild, motherByChild);
                    return husbandOfElderMatrilineToYoungerHusbandReverse(other, viewer, dViewerUpToOtherWife, side);
                }
            }
        }

        return null;
    }

    /**
     * Chồng/ông trên dòng máu nam ↔ vợ của hậu duệ (dâu): cùng nhãn với khi so vợ–dâu (cháu dâu, chắt dâu…).
     * Đối xứng: bà/mẹ dòng máu nữ ↔ chồng của hậu duệ nữ (cháu rể…).
     */
    private String patrilineOrMatrilineBloodToSpouseOfDescendantKinship(
            String viewerId,
            String otherId,
            Map<String, User> userById,
            Map<String, Set<String>> parentsByChild,
            Map<String, String> fatherByChild,
            Map<String, String> motherByChild,
            Map<String, Set<String>> spousesByMember
    ) {
        User viewer = userById.get(viewerId);
        User other = userById.get(otherId);
        if (viewer == null || other == null) {
            return null;
        }

        if ("MALE".equalsIgnoreCase(upper(viewer.getGender()))
                && "FEMALE".equalsIgnoreCase(upper(other.getGender()))) {
            String hOther = maleSpouseId(otherId, spousesByMember, userById);
            if (hOther != null && !hOther.equals(viewerId)) {
                Integer d = collectAncestorDistance(hOther, parentsByChild, 24).get(viewerId);
                if (d != null && d >= 1) {
                    return wifeOfElderPatrilineToYoungerWifeForward(d);
                }
            }
            String wViewer = femaleSpouseId(viewerId, spousesByMember, userById);
            if (wViewer != null && !wViewer.equals(otherId)) {
                Integer dMatRev = collectAncestorDistance(wViewer, parentsByChild, 24).get(otherId);
                if (dMatRev != null && dMatRev >= 1) {
                    if (dMatRev == 1) {
                        return "Mẹ vợ";
                    }
                    String side = lineageSide(otherId, wViewer, fatherByChild, motherByChild);
                    return ancestorLabelAffine(other, viewer, dMatRev, side);
                }
            }
        }

        if ("FEMALE".equalsIgnoreCase(upper(viewer.getGender()))
                && "MALE".equalsIgnoreCase(upper(other.getGender()))) {
            String hViewer = maleSpouseId(viewerId, spousesByMember, userById);
            if (hViewer != null && !hViewer.equals(otherId)) {
                Integer dPat = collectAncestorDistance(hViewer, parentsByChild, 24).get(otherId);
                if (dPat != null && dPat >= 1) {
                    if (dPat == 1) {
                        return "Cha chồng";
                    }
                    String side = lineageSide(otherId, hViewer, fatherByChild, motherByChild);
                    return ancestorLabelAffine(other, viewer, dPat, side);
                }
            }
            String wOther = femaleSpouseId(otherId, spousesByMember, userById);
            if (wOther != null && !wOther.equals(viewerId)) {
                Integer dMat = collectAncestorDistance(wOther, parentsByChild, 24).get(viewerId);
                if (dMat != null && dMat >= 1) {
                    return husbandOfElderMatrilineToYoungerHusbandForward(dMat);
                }
            }
        }

        return null;
    }

    /** Người xem là vợ tổ tiên nam; đối phương là vợ hậu duệ máu của chồng mình. {@code generations} = số bậc từ chồng đối phương lên đến chồng người xem. */
    private static String wifeOfElderPatrilineToYoungerWifeForward(int generations) {
        return switch (generations) {
            case 1 -> "Con dâu";
            case 2 -> "Cháu dâu";
            case 3 -> "Chắt dâu";
            case 4 -> "Chít dâu";
            default -> "Hậu duệ (dâu)";
        };
    }

    /** Người xem là vợ đời sau; đối phương là vợ tổ tiên trên nhánh chồng. */
    private String wifeOfElderPatrilineToYoungerWifeReverse(User elderWife, User juniorWife, int generations, String side) {
        if (generations == 1) {
            return "Mẹ chồng";
        }
        return ancestorLabelAffine(elderWife, juniorWife, generations, side);
    }

    private static String husbandOfElderMatrilineToYoungerHusbandForward(int generations) {
        return switch (generations) {
            case 1 -> "Con rể";
            case 2 -> "Cháu rể";
            case 3 -> "Chắt rể";
            case 4 -> "Chít rể";
            default -> "Hậu duệ (rể)";
        };
    }

    private String husbandOfElderMatrilineToYoungerHusbandReverse(User elderHusband, User juniorHusband, int generations, String side) {
        if (generations == 1) {
            return "Bố vợ";
        }
        return ancestorLabelAffine(elderHusband, juniorHusband, generations, side);
    }

    /**
     * Quan hệ qua nhà chồng / nhà vợ: con dâu (nữ + chồng nam) và con rể (nam + vợ nữ), kể cả cháu gọi bác dâu/thím/cô.
     */
    private String affineSpouseFamilyKinship(
            String viewerId,
            String otherId,
            Map<String, User> userById,
            Map<String, Set<String>> parentsByChild,
            Map<String, String> fatherByChild,
            Map<String, String> motherByChild,
            Map<String, Set<String>> spousesByMember
    ) {
        User v = userById.get(viewerId);
        User w = userById.get(otherId);
        if (v == null || w == null) {
            return null;
        }

        if ("FEMALE".equalsIgnoreCase(upper(v.getGender()))) {
            String hId = maleSpouseId(viewerId, spousesByMember, userById);
            if (hId != null) {
                String r = daughterInLawViewOfOther(viewerId, otherId, hId, userById, parentsByChild, fatherByChild, motherByChild, spousesByMember);
                if (r != null) {
                    return r;
                }
            }
        }

        String childCallsDaughterInLaw = childViewOfDaughterInLawAunt(viewerId, otherId, userById, parentsByChild, spousesByMember);
        if (childCallsDaughterInLaw != null) {
            return childCallsDaughterInLaw;
        }

        if ("MALE".equalsIgnoreCase(upper(v.getGender()))) {
            String wId = femaleSpouseId(viewerId, spousesByMember, userById);
            if (wId != null) {
                String r = sonInLawViewOfOther(viewerId, otherId, wId, userById, parentsByChild, fatherByChild, motherByChild, spousesByMember);
                if (r != null) {
                    return r;
                }
            }
        }

        String childCallsSonInLaw = childViewOfSonInLawUncle(viewerId, otherId, userById, parentsByChild, spousesByMember);
        if (childCallsSonInLaw != null) {
            return childCallsSonInLaw;
        }

        return null;
    }

    private static String maleSpouseId(String personId, Map<String, Set<String>> spousesByMember, Map<String, User> userById) {
        for (String sid : spousesByMember.getOrDefault(personId, Set.of())) {
            User s = userById.get(sid);
            if (s != null && "MALE".equalsIgnoreCase(upper(s.getGender()))) {
                return sid;
            }
        }
        return null;
    }

    private static String femaleSpouseId(String personId, Map<String, Set<String>> spousesByMember, Map<String, User> userById) {
        for (String sid : spousesByMember.getOrDefault(personId, Set.of())) {
            User s = userById.get(sid);
            if (s != null && "FEMALE".equalsIgnoreCase(upper(s.getGender()))) {
                return sid;
            }
        }
        return null;
    }

    /** Cách người xem (con dâu) gọi {@code other}, chồng là {@code hId}. */
    private String daughterInLawViewOfOther(
            String viewerId,
            String otherId,
            String hId,
            Map<String, User> userById,
            Map<String, Set<String>> parentsByChild,
            Map<String, String> fatherByChild,
            Map<String, String> motherByChild,
            Map<String, Set<String>> spousesByMember
    ) {
        if (otherId.equals(hId)) {
            return null;
        }

        User h = userById.get(hId);
        User o = userById.get(otherId);
        if (h == null || o == null) {
            return null;
        }

        Set<String> hParents = parentsByChild.getOrDefault(hId, Set.of());
        if (hParents.contains(otherId)) {
            return "MALE".equalsIgnoreCase(upper(o.getGender())) ? "Cha chồng" : "Mẹ chồng";
        }

        if (areSiblings(otherId, hId, parentsByChild)) {
            return spouseSiblingLabelFromWifePerspective(o, h);
        }

        for (String sId : bloodSiblingIds(hId, parentsByChild)) {
            if (sId.equals(hId) || sId.equals(viewerId)) {
                continue;
            }
            if (spousesByMember.getOrDefault(sId, Set.of()).contains(otherId)) {
                User s = userById.get(sId);
                if (s == null) {
                    continue;
                }
                return daughterInLawToSpouseOfHusbandSibling(o, s, h);
            }
        }

        for (String sId : bloodSiblingIds(hId, parentsByChild)) {
            if (parentsByChild.getOrDefault(otherId, Set.of()).contains(sId)) {
                User s = userById.get(sId);
                if (s == null) {
                    continue;
                }
                return daughterInLawToChildOfHusbandSibling(s, h);
            }
        }

        String fId = fatherByChild.get(hId);
        if (fId != null) {
            if (areSiblings(otherId, fId, parentsByChild)) {
                return paternalExtendedOnHusbandSide(o, userById.get(fId));
            }
            for (String uId : bloodSiblingIds(fId, parentsByChild)) {
                if (spousesByMember.getOrDefault(uId, Set.of()).contains(otherId)) {
                    User u = userById.get(uId);
                    User fil = userById.get(fId);
                    if (u != null && fil != null) {
                        return wifeOrHusbandOfPaternalKinOfHusband(o, u, fil);
                    }
                }
            }
        }

        String mId = motherByChild.get(hId);
        if (mId != null) {
            if (areSiblings(otherId, mId, parentsByChild)) {
                return maternalExtendedOnHusbandSide(o, userById.get(mId));
            }
            for (String uId : bloodSiblingIds(mId, parentsByChild)) {
                if (spousesByMember.getOrDefault(uId, Set.of()).contains(otherId)) {
                    User u = userById.get(uId);
                    User mil = userById.get(mId);
                    if (u != null && mil != null) {
                        return spouseOfMaternalKinOfHusband(o, u, mil);
                    }
                }
            }
        }

        return null;
    }

    /** Anh/chị/em chồng (anh em ruột của chồng). */
    private String spouseSiblingLabelFromWifePerspective(User sibling, User husband) {
        String gs = upper(sibling.getGender());
        if ("MALE".equals(gs)) {
            return isOlder(sibling, husband) ? "Anh chồng" : "Em chồng";
        }
        if ("FEMALE".equals(gs)) {
            return isOlder(sibling, husband) ? "Chị chồng" : "Em chồng";
        }
        return "Anh/chị/em chồng";
    }

    /** Vợ/chồng của anh/chị/em chồng. */
    private String daughterInLawToSpouseOfHusbandSibling(User otherSpouse, User siblingOfH, User husband) {
        String gs = upper(siblingOfH.getGender());
        String go = upper(otherSpouse.getGender());
        boolean sOlder = isOlder(siblingOfH, husband);
        if ("MALE".equals(gs) && "FEMALE".equals(go)) {
            return sOlder ? "Chị dâu" : "Em dâu";
        }
        if ("FEMALE".equals(gs) && "MALE".equals(go)) {
            return sOlder ? "Anh rể" : "Em rể";
        }
        return sOlder ? "Chị/em dâu - anh/em rể (bên chồng)" : "Thông gia (bên chồng)";
    }

    /**
     * Con của anh/chị/em chồng — người xem là con dâu: gọi là cháu; vai của mình: bác dâu / thím / cô.
     */
    private String daughterInLawToChildOfHusbandSibling(User siblingOfH, User husband) {
        String gs = upper(siblingOfH.getGender());
        if ("MALE".equals(gs)) {
            String role = isOlder(siblingOfH, husband) ? "bác dâu" : "thím";
            return "Cháu (" + role + ")";
        }
        if ("FEMALE".equals(gs)) {
            return "Cháu (cô)";
        }
        return "Cháu (bên chồng)";
    }

    private String paternalExtendedOnHusbandSide(User uncleOrAunt, User fatherInLaw) {
        if (uncleOrAunt == null || fatherInLaw == null) {
            return null;
        }
        String g = upper(uncleOrAunt.getGender());
        if ("FEMALE".equals(g)) {
            return "Cô chồng";
        }
        if ("MALE".equals(g)) {
            return isOlder(uncleOrAunt, fatherInLaw) ? "Bác chồng" : "Chú chồng";
        }
        return "Bác/chú/cô chồng";
    }

    private String maternalExtendedOnHusbandSide(User kin, User motherInLaw) {
        if (kin == null || motherInLaw == null) {
            return null;
        }
        String g = upper(kin.getGender());
        if ("FEMALE".equals(g)) {
            return "Dì chồng";
        }
        if ("MALE".equals(g)) {
            return "Cậu chồng";
        }
        return "Cậu/dì chồng";
    }

    /** Vợ/chồng của bác/chú/cô (bên nội chồng). */
    private String wifeOrHusbandOfPaternalKinOfHusband(User other, User u, User fatherInLaw) {
        String gu = upper(u.getGender());
        String go = upper(other.getGender());
        if ("MALE".equals(gu) && "FEMALE".equals(go)) {
            return isOlder(u, fatherInLaw) ? "Bác gái (chồng)" : "Thím chồng";
        }
        if ("FEMALE".equals(gu) && "MALE".equals(go)) {
            return "Dượng chồng";
        }
        return "Họ hàng bên nội chồng";
    }

    /** Vợ/chồng của cậu/dì (bên ngoại chồng). */
    private String spouseOfMaternalKinOfHusband(User other, User u, User motherInLaw) {
        String gu = upper(u.getGender());
        String go = upper(other.getGender());
        if ("MALE".equals(gu) && "FEMALE".equals(go)) {
            return "Mợ chồng";
        }
        if ("FEMALE".equals(gu) && "MALE".equals(go)) {
            return "Dượng chồng (bên mẹ chồng)";
        }
        return "Họ hàng bên mẹ chồng";
    }

    /** Cháu (máu) gọi con dâu là bác dâu / thím / cô. */
    private String childViewOfDaughterInLawAunt(
            String viewerId,
            String otherId,
            Map<String, User> userById,
            Map<String, Set<String>> parentsByChild,
            Map<String, Set<String>> spousesByMember
    ) {
        User other = userById.get(otherId);
        if (other == null) {
            return null;
        }
        String hId = maleSpouseId(otherId, spousesByMember, userById);
        if (hId == null || !"FEMALE".equalsIgnoreCase(upper(other.getGender()))) {
            return null;
        }
        Set<String> cParents = parentsByChild.getOrDefault(viewerId, Set.of());
        for (String sId : cParents) {
            if (areSiblings(sId, hId, parentsByChild)) {
                User s = userById.get(sId);
                User h = userById.get(hId);
                if (s == null || h == null) {
                    continue;
                }
                String gs = upper(s.getGender());
                if ("MALE".equals(gs)) {
                    return isOlder(s, h) ? "Bác dâu" : "Thím";
                }
                if ("FEMALE".equals(gs)) {
                    return "Cô (dâu)";
                }
                return "Cô/dâu (bên chồng)";
            }
        }
        return null;
    }

    /** Con rể: bố/mẹ vợ, anh/chị/em vợ, cháu bên vợ, họ mở rộng bên vợ. */
    private String sonInLawViewOfOther(
            String viewerId,
            String otherId,
            String wId,
            Map<String, User> userById,
            Map<String, Set<String>> parentsByChild,
            Map<String, String> fatherByChild,
            Map<String, String> motherByChild,
            Map<String, Set<String>> spousesByMember
    ) {
        if (otherId.equals(wId)) {
            return null;
        }

        User w = userById.get(wId);
        User o = userById.get(otherId);
        if (w == null || o == null) {
            return null;
        }

        Set<String> wParents = parentsByChild.getOrDefault(wId, Set.of());
        if (wParents.contains(otherId)) {
            return "MALE".equalsIgnoreCase(upper(o.getGender())) ? "Bố vợ" : "Mẹ vợ";
        }

        if (areSiblings(otherId, wId, parentsByChild)) {
            return spouseSiblingLabelFromHusbandPerspective(o, w);
        }

        for (String sId : bloodSiblingIds(wId, parentsByChild)) {
            if (sId.equals(wId) || sId.equals(viewerId)) {
                continue;
            }
            if (spousesByMember.getOrDefault(sId, Set.of()).contains(otherId)) {
                User s = userById.get(sId);
                if (s == null) {
                    continue;
                }
                return sonInLawToSpouseOfWifeSibling(o, s, w);
            }
        }

        for (String sId : bloodSiblingIds(wId, parentsByChild)) {
            if (parentsByChild.getOrDefault(otherId, Set.of()).contains(sId)) {
                User s = userById.get(sId);
                if (s == null) {
                    continue;
                }
                return sonInLawToChildOfWifeSibling(s, w);
            }
        }

        String fId = fatherByChild.get(wId);
        if (fId != null) {
            if (areSiblings(otherId, fId, parentsByChild)) {
                return paternalExtendedOnWifeSide(o, userById.get(fId));
            }
            for (String uId : bloodSiblingIds(fId, parentsByChild)) {
                if (spousesByMember.getOrDefault(uId, Set.of()).contains(otherId)) {
                    User u = userById.get(uId);
                    User fil = userById.get(fId);
                    if (u != null && fil != null) {
                        return wifeOrHusbandOfPaternalKinOfWife(o, u, fil);
                    }
                }
            }
        }

        String mId = motherByChild.get(wId);
        if (mId != null) {
            if (areSiblings(otherId, mId, parentsByChild)) {
                return maternalExtendedOnWifeSide(o, userById.get(mId));
            }
            for (String uId : bloodSiblingIds(mId, parentsByChild)) {
                if (spousesByMember.getOrDefault(uId, Set.of()).contains(otherId)) {
                    User u = userById.get(uId);
                    User mil = userById.get(mId);
                    if (u != null && mil != null) {
                        return spouseOfMaternalKinOfWife(o, u, mil);
                    }
                }
            }
        }

        return null;
    }

    private String spouseSiblingLabelFromHusbandPerspective(User sibling, User wife) {
        String gs = upper(sibling.getGender());
        if ("MALE".equals(gs)) {
            return isOlder(sibling, wife) ? "Anh vợ" : "Em vợ";
        }
        if ("FEMALE".equals(gs)) {
            return isOlder(sibling, wife) ? "Chị vợ" : "Em vợ";
        }
        return "Anh/chị/em vợ";
    }

    private String sonInLawToSpouseOfWifeSibling(User otherSpouse, User siblingOfW, User wife) {
        String gs = upper(siblingOfW.getGender());
        String go = upper(otherSpouse.getGender());
        boolean sOlder = isOlder(siblingOfW, wife);
        if ("MALE".equals(gs) && "FEMALE".equals(go)) {
            return sOlder ? "Chị dâu (bên vợ)" : "Em dâu (bên vợ)";
        }
        if ("FEMALE".equals(gs) && "MALE".equals(go)) {
            return sOlder ? "Anh rể (bên vợ)" : "Em rể (bên vợ)";
        }
        return "Thông gia (bên vợ)";
    }

    private String sonInLawToChildOfWifeSibling(User siblingOfW, User wife) {
        String gs = upper(siblingOfW.getGender());
        if ("MALE".equals(gs)) {
            String role = isOlder(siblingOfW, wife) ? "bác vợ" : "chú vợ";
            return "Cháu (" + role + ")";
        }
        if ("FEMALE".equals(gs)) {
            return "Cháu (cô vợ)";
        }
        return "Cháu (bên vợ)";
    }

    private String paternalExtendedOnWifeSide(User kin, User fatherInLaw) {
        if (kin == null || fatherInLaw == null) {
            return null;
        }
        String g = upper(kin.getGender());
        if ("FEMALE".equals(g)) {
            return "Cô vợ";
        }
        if ("MALE".equals(g)) {
            return isOlder(kin, fatherInLaw) ? "Bác vợ" : "Chú vợ";
        }
        return "Bác/chú/cô vợ";
    }

    private String maternalExtendedOnWifeSide(User kin, User motherInLaw) {
        if (kin == null || motherInLaw == null) {
            return null;
        }
        String g = upper(kin.getGender());
        if ("FEMALE".equals(g)) {
            return "Dì vợ";
        }
        if ("MALE".equals(g)) {
            return "Cậu vợ";
        }
        return "Cậu/dì vợ";
    }

    private String wifeOrHusbandOfPaternalKinOfWife(User other, User u, User fatherInLaw) {
        String gu = upper(u.getGender());
        String go = upper(other.getGender());
        if ("MALE".equals(gu) && "FEMALE".equals(go)) {
            return isOlder(u, fatherInLaw) ? "Bác gái (vợ)" : "Thím vợ";
        }
        if ("FEMALE".equals(gu) && "MALE".equals(go)) {
            return "Dượng vợ";
        }
        return "Họ hàng bên nội vợ";
    }

    private String spouseOfMaternalKinOfWife(User other, User u, User motherInLaw) {
        String gu = upper(u.getGender());
        String go = upper(other.getGender());
        if ("MALE".equals(gu) && "FEMALE".equals(go)) {
            return "Mợ vợ";
        }
        if ("FEMALE".equals(gu) && "MALE".equals(go)) {
            return "Dượng vợ (bên mẹ vợ)";
        }
        return "Họ hàng bên mẹ vợ";
    }

    /** Cháu gọi con rể (chú rể / bác rể / dượng tùy vai). */
    private String childViewOfSonInLawUncle(
            String viewerId,
            String otherId,
            Map<String, User> userById,
            Map<String, Set<String>> parentsByChild,
            Map<String, Set<String>> spousesByMember
    ) {
        User other = userById.get(otherId);
        if (other == null) {
            return null;
        }
        String wifeId = femaleSpouseId(otherId, spousesByMember, userById);
        if (wifeId == null || !"MALE".equalsIgnoreCase(upper(other.getGender()))) {
            return null;
        }
        Set<String> cParents = parentsByChild.getOrDefault(viewerId, Set.of());
        for (String sId : cParents) {
            if (areSiblings(sId, wifeId, parentsByChild)) {
                User s = userById.get(sId);
                User w = userById.get(wifeId);
                if (s == null || w == null) {
                    continue;
                }
                String gs = upper(s.getGender());
                if ("MALE".equals(gs)) {
                    return isOlder(s, w) ? "Bác rể" : "Chú rể";
                }
                if ("FEMALE".equals(gs)) {
                    return "Dượng (vợ)";
                }
                return "Chú/dượng (bên vợ)";
            }
        }
        return null;
    }

    private Set<String> bloodSiblingIds(String personId, Map<String, Set<String>> parentsByChild) {
        Set<String> pset = parentsByChild.getOrDefault(personId, Set.of());
        Set<String> sibs = new LinkedHashSet<>();
        for (Map.Entry<String, Set<String>> e : parentsByChild.entrySet()) {
            String child = e.getKey();
            if (child.equals(personId)) {
                continue;
            }
            for (String p : e.getValue()) {
                if (pset.contains(p)) {
                    sibs.add(child);
                    break;
                }
            }
        }
        return sibs;
    }

    /**
     * Bổ sung cạnh cha–con từ {@link User#getParentId()} khi đồng bộ {@code Relationship} chưa có hoặc chưa nạp đủ,
     * để so sánh quan hệ / tổ tiên chung khớp với cây và hồ sơ.
     */
    private void mergeParentIdColumnIntoKinshipGraph(
            Map<String, User> userById,
            Map<String, Set<String>> parentsByChild,
            Map<String, String> fatherByChild,
            Map<String, String> motherByChild
    ) {
        for (User child : userById.values()) {
            if (child == null) {
                continue;
            }
            String pid = child.getParentId();
            if (pid == null || pid.isBlank()) {
                continue;
            }
            User parent = userById.get(pid.trim());
            if (parent == null) {
                continue;
            }
            String cid = child.getUserId();
            String puid = parent.getUserId();
            parentsByChild.computeIfAbsent(cid, k -> new HashSet<>()).add(puid);
            if (parent.getGender() != null) {
                if ("MALE".equalsIgnoreCase(parent.getGender())) {
                    fatherByChild.putIfAbsent(cid, puid);
                } else if ("FEMALE".equalsIgnoreCase(parent.getGender())) {
                    motherByChild.putIfAbsent(cid, puid);
                }
            }
        }
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