package com.family.app.service;

import com.family.app.dto.FamilyResponse;
import com.family.app.dto.FamilyWriteRequest;
import com.family.app.dto.CreateSpouseRequest;
import com.family.app.dto.MemberRoleOptionResponse;
import com.family.app.dto.UserRequest;
import com.family.app.dto.UserResponse;
import com.family.app.model.Family;
import com.family.app.model.Permission;
import com.family.app.model.Relationship;
import com.family.app.model.Role;
import com.family.app.model.User;
import com.family.app.security.UserRoleSupport;
import com.family.app.security.AppPermissions;
import com.family.app.repository.FamilyRepository;
import com.family.app.repository.RelationshipRepository;
import com.family.app.repository.RoleRepository;
import com.family.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FamilyHeadService {

    private static final Set<String> ASSIGNABLE_MEMBER_ROLE_NAMES = Set.of(
            "MEMBER", "FAMILY_NEWS_MANAGER", "FAMILY_BRANCH_MANAGER");

    /** Không gán qua màn trưởng họ (quản trị hệ thống). */
    private static final Set<String> NON_ASSIGNABLE_FAMILY_UI_ROLE_NAMES = Set.of("ADMIN");

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private FamilyRepository familyRepository;
    @Autowired
    private FamilyScopeService familyScopeService;
    @Autowired
    private RelationshipRepository relationshipRepository;

    /** Chi của tài khoản và mọi chi con (cho dropdown / lọc). */
    /**
     * @param forMemberAdmin true: phạm vi quản lý thành viên/chi; false: phạm vi tin (mở rộng nếu có FAMILY_NEWS_MANAGER).
     */
    @Transactional(readOnly = true)
    public List<FamilyResponse> listFamiliesInScope(String managerUserId, boolean forMemberAdmin) {
        Set<String> ids = forMemberAdmin
                ? familyScopeService.manageableFamilyIdsForMembers(managerUserId)
                : familyScopeService.manageableFamilyIdsForNews(managerUserId);
        return ids.stream()
                .map(id -> familyRepository.findByIdWithParentFamily(id).orElse(null))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Family::getFamilyName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(this::mapFamily)
                .collect(Collectors.toList());
    }

    public FamilyResponse createFamily(FamilyWriteRequest req) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Trưởng họ không thể tạo dòng họ mới.");
    }

    /**
     * Sửa tên / mô tả / quyền riêng tư của một chi trong phạm vi nhánh.
     */
    @Transactional
    public FamilyResponse updateFamily(String id, FamilyWriteRequest req, String managerUserId) {
        familyScopeService.assertCanManageFamilyMembers(managerUserId, id);
        Family f = familyRepository.findByIdWithParentFamily(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dòng họ: " + id));
        if (req.getFamilyName() != null && !req.getFamilyName().isBlank()) {
            f.setFamilyName(req.getFamilyName().trim());
        }
        if (req.getDescription() != null) {
            f.setDescription(req.getDescription());
        }
        if (req.getPrivacySetting() != null) {
            f.setPrivacySetting(req.getPrivacySetting().isBlank() ? "PUBLIC" : req.getPrivacySetting().trim());
        }
        return mapFamily(familyRepository.save(f));
    }

    public void deleteFamily(String id, String managerUserId) {
        familyScopeService.assertCanManageFamilyMembers(managerUserId, id);
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không thể xóa dòng họ từ tài khoản trưởng họ.");
    }

    /** Vai trò có thể chọn khi thêm/sửa thành viên (theo nhánh dòng họ). */
    @Transactional(readOnly = true)
    public List<MemberRoleOptionResponse> listAssignableMemberRoles() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("MEMBER", "Thành viên");
        labels.put("FAMILY_NEWS_MANAGER", "Phụ trách tin & sự kiện");
        labels.put("FAMILY_BRANCH_MANAGER", "Phụ trách chi (tin + thành viên trong nhánh)");
        List<MemberRoleOptionResponse> out = new ArrayList<>();
        for (Map.Entry<String, String> e : labels.entrySet()) {
            Role r = roleRepository.findByRoleName(e.getKey()).orElse(null);
            if (r == null) {
                continue;
            }
            out.add(MemberRoleOptionResponse.builder()
                    .roleId(r.getRoleId())
                    .roleName(r.getRoleName())
                    .label(e.getValue())
                    .build());
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getMembersForManagedFamily(String familyId) {
        Map<String, String> spouseByUser = buildSpousePartnerMap(familyId);
        return userRepository.findByFamily_FamilyIdOrderByOrderInFamilyAsc(familyId).stream()
                .map(u -> mapToResponse(u, spouseByUser))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponse getMember(String id, String managerUserId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thành viên có ID: " + id));
        if (user.getFamily() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Thành viên không gắn dòng họ.");
        }
        familyScopeService.assertCanManageFamilyMembers(managerUserId, user.getFamily().getFamilyId());
        Map<String, String> spouseByUser = buildSpousePartnerMap(user.getFamily().getFamilyId());
        return mapToResponse(user, spouseByUser);
    }

    @Transactional
    public UserResponse createSpouseForMember(String partnerId, CreateSpouseRequest req, String managerUserId) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu dữ liệu.");
        }
        if (req.getFullName() == null || req.getFullName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nhập họ tên vợ/chồng.");
        }
        String newGender = req.getGender() != null ? req.getGender().trim() : "";
        if (!"MALE".equals(newGender) && !"FEMALE".equals(newGender)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giới tính vợ/chồng phải là Nam hoặc Nữ.");
        }

        User partner = userRepository.findById(partnerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thành viên."));
        if (partner.getFamily() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thành viên chưa gắn dòng họ.");
        }
        String familyId = partner.getFamily().getFamilyId();
        familyScopeService.assertCanManageFamilyMembers(managerUserId, familyId);

        String partnerGender = partner.getGender() != null ? partner.getGender().trim() : "";
        if (!"MALE".equals(partnerGender) && !"FEMALE".equals(partnerGender)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cần cập nhật giới tính Nam/Nữ cho thành viên này trước (Sửa hồ sơ).");
        }
        if (partnerGender.equals(newGender)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Vợ chồng phải khác giới (nam với nữ), không gắn nam–nam hoặc nữ–nữ.");
        }
        if (!relationshipRepository.findSpouses(partnerId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Thành viên này đã có vợ/chồng.");
        }
        if (req.getDob() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bắt buộc nhập ngày sinh cho vợ/chồng.");
        }
        String ph = req.getPhoneNumber() != null ? req.getPhoneNumber().trim() : "";
        if (ph.isEmpty() || !ph.matches("^[0-9]{10}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại bắt buộc, đúng 10 chữ số.");
        }

        int gen = partner.getGeneration() != null ? partner.getGeneration() : 1;
        int maxOrder = 0;
        for (User u : userRepository.findByFamily_FamilyIdOrderByOrderInFamilyAsc(familyId)) {
            int ug = u.getGeneration() != null ? u.getGeneration() : 1;
            if (ug != gen) {
                continue;
            }
            int o = u.getOrderInFamily() != null ? u.getOrderInFamily() : 0;
            maxOrder = Math.max(maxOrder, o);
        }

        User spouse = new User();
        spouse.setFullName(req.getFullName().trim());
        spouse.setEmail(blankToNull(req.getEmail()));
        spouse.setGender(newGender);
        spouse.setDob(req.getDob());
        spouse.setPhoneNumber(blankToNull(req.getPhoneNumber()));
        spouse.setHometown(blankToNull(req.getHometown()));
        spouse.setCurrentAddress(blankToNull(req.getCurrentAddress()));
        spouse.setOccupation(blankToNull(req.getOccupation()));
        spouse.setBio(blankToNull(req.getBio()));
        spouse.setFamily(partner.getFamily());
        spouse.setGeneration(gen);
        spouse.setOrderInFamily(maxOrder + 1);
        spouse.setParentId(null);
        spouse.setPassword(passwordEncoder.encode("123456"));
        spouse.setStatus(1);

        Role memberRole = roleRepository.findByRoleName("MEMBER")
                .orElseThrow(() -> new RuntimeException("Chưa cấu hình Role MEMBER trong DB!"));
        spouse.setRoles(new LinkedHashSet<>(Set.of(memberRole)));

        User savedSpouse = userRepository.save(spouse);

        Relationship rel = new Relationship();
        rel.setPerson1(partner);
        rel.setPerson2(savedSpouse);
        rel.setRelType("SPOUSE");
        relationshipRepository.save(rel);

        return mapToResponse(savedSpouse, buildSpousePartnerMap(familyId));
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    /** Khi tạo thành viên mới: bắt buộc giới tính, ngày sinh, SĐT 10 số (đồng bộ FE). */
    private void validateRequiredMemberFieldsForCreate(UserRequest request) {
        String g = request.getGender() != null ? request.getGender().trim() : "";
        if (g.isEmpty() || (!"MALE".equals(g) && !"FEMALE".equals(g))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bắt buộc chọn giới tính (Nam hoặc Nữ).");
        }
        if (request.getDob() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bắt buộc nhập ngày sinh.");
        }
        String phone = request.getPhoneNumber() != null ? request.getPhoneNumber().trim() : "";
        if (phone.isEmpty() || !phone.matches("^[0-9]{10}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại bắt buộc, đúng 10 chữ số.");
        }
    }

    @Transactional
    public UserResponse saveMember(UserRequest request, String managerUserId) {
        if (request.getFamilyId() == null || request.getFamilyId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu familyId.");
        }
        validateRequiredMemberFieldsForCreate(request);
        familyScopeService.assertCanManageFamilyMembers(managerUserId, request.getFamilyId().trim());

        String managedFamilyId = request.getFamilyId().trim();
        User user = new User();
        mapRequestToEntity(user, request);

        user.setPassword(passwordEncoder.encode("123456"));
        user.setStatus(1);

        if (request.getParentId() != null && !request.getParentId().isBlank()) {
            String pid = request.getParentId().trim();
            User parent = userRepository.findById(pid)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy cha/mẹ."));
            if (parent.getFamily() == null
                    || !managedFamilyId.equals(parent.getFamily().getFamilyId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cha/mẹ phải cùng dòng họ đang thêm.");
            }
            if (relationshipRepository.findSpouses(pid).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cha/mẹ phải đã có vợ/chồng trước khi thêm con.");
            }
            int parentGen = parent.getGeneration() != null ? parent.getGeneration() : 1;
            user.setGeneration(parentGen + 1);
        } else if (user.getGeneration() == null) {
            user.setGeneration(1);
        }

        applyMemberRole(user, request, null, managerUserId);

        User savedUser = userRepository.save(user);
        syncParentChildEdgesForChild(savedUser);
        return mapToResponse(savedUser, buildSpousePartnerMap(managedFamilyId));
    }

    @Transactional
    public UserResponse updateMember(String id, UserRequest request, String managerUserId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thành viên có ID: " + id));
        if (user.getFamily() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không thể sửa thành viên ngoài dòng họ.");
        }
        familyScopeService.assertCanManageFamilyMembers(managerUserId, user.getFamily().getFamilyId());
        if (request.getFamilyId() != null && !request.getFamilyId().isBlank()) {
            familyScopeService.assertCanManageFamilyMembers(managerUserId, request.getFamilyId().trim());
        }
        request.setFamilyId(request.getFamilyId() != null && !request.getFamilyId().isBlank()
                ? request.getFamilyId().trim()
                : user.getFamily().getFamilyId());
        mapRequestToEntity(user, request);

        String pid = user.getParentId();
        if (pid != null && !pid.isBlank()) {
            pid = pid.trim();
            User parent = userRepository.findById(pid)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy cha/mẹ."));
            if (parent.getFamily() == null
                    || !user.getFamily().getFamilyId().equals(parent.getFamily().getFamilyId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cha/mẹ phải cùng dòng họ.");
            }
            if (relationshipRepository.findSpouses(pid).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cha/mẹ phải đã có vợ/chồng trước khi gắn con.");
            }
        }

        applyMemberRole(user, request, id, managerUserId);

        User updatedUser = userRepository.save(user);
        syncParentChildEdgesForChild(updatedUser);
        return mapToResponse(updatedUser, buildSpousePartnerMap(user.getFamily().getFamilyId()));
    }

    /**
     * Trang công khai và cây gia phả đọc cha/mẹ qua {@code Relationship} kiểu PARENT_CHILD (person1 = cha/mẹ, person2 = con).
     * Khi lưu chỉ cập nhật {@code User.parentId} thì phải đồng bộ các cạnh này.
     */
    private void syncParentChildEdgesForChild(User child) {
        String childId = child.getUserId();
        if (childId == null) {
            return;
        }
        relationshipRepository.deleteParentChildLinksToChild(childId);
        String pid = child.getParentId();
        if (pid == null || pid.isBlank()) {
            return;
        }
        pid = pid.trim();
        LinkedHashSet<String> parentIds = new LinkedHashSet<>();
        parentIds.add(pid);
        for (Relationship sr : relationshipRepository.findSpouses(pid)) {
            String partner = spousePartnerUserId(sr, pid);
            if (partner != null) {
                parentIds.add(partner);
            }
        }
        for (String parentUserId : parentIds) {
            ensureParentChildRelationship(parentUserId, childId);
        }
    }

    private static String spousePartnerUserId(Relationship spouseRel, String oneEndUserId) {
        if (spouseRel.getPerson1() != null && oneEndUserId.equals(spouseRel.getPerson1().getUserId())) {
            return spouseRel.getPerson2() != null ? spouseRel.getPerson2().getUserId() : null;
        }
        if (spouseRel.getPerson2() != null && oneEndUserId.equals(spouseRel.getPerson2().getUserId())) {
            return spouseRel.getPerson1() != null ? spouseRel.getPerson1().getUserId() : null;
        }
        return null;
    }

    private void ensureParentChildRelationship(String parentUserId, String childUserId) {
        if (parentUserId == null || childUserId == null) {
            return;
        }
        if (relationshipRepository.existsByRelTypeAndPerson1_UserIdAndPerson2_UserId(
                "PARENT_CHILD", parentUserId, childUserId)) {
            return;
        }
        User p = userRepository.findById(parentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy cha/mẹ."));
        User c = userRepository.findById(childUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy con."));
        if (p.getFamily() == null || c.getFamily() == null
                || !p.getFamily().getFamilyId().equals(c.getFamily().getFamilyId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cha/mẹ và con phải cùng dòng họ.");
        }
        Relationship r = new Relationship();
        r.setPerson1(p);
        r.setPerson2(c);
        r.setRelType("PARENT_CHILD");
        relationshipRepository.save(r);
    }

    /**
     * Xóa thành viên khi không còn con (theo parent_id).
     * Có quan hệ vợ/chồng: không cho xóa nếu là nam (chồng); chỉ khi xóa nữ (vợ) mới gỡ SPOUSE và xóa user.
     */
    @Transactional
    public void deleteMember(String id, String managerUserId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thành viên không tồn tại."));
        if (user.getFamily() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không thể xóa thành viên ngoài dòng họ.");
        }
        familyScopeService.assertCanManageFamilyMembers(managerUserId, user.getFamily().getFamilyId());

        if (userRepository.existsByParentId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Không thể xóa khi còn con/cháu. Hãy xóa từ thế hệ sau (con, cháu…) lên trước, hoặc đổi cha/mẹ cho các con.");
        }

        if (!relationshipRepository.findSpouses(id).isEmpty()) {
            String g = user.getGender() != null ? user.getGender().trim() : "";
            if (!"FEMALE".equals(g)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Không thể xóa chồng khi còn vợ. Chỉ có thể xóa vợ để gỡ quan hệ vợ chồng; muốn xóa chồng cần xóa vợ trước.");
            }
        }

        relationshipRepository.deleteAllInvolvingUserId(id);
        userRepository.delete(user);
    }

    private FamilyResponse mapFamily(Family f) {
        FamilyResponse r = new FamilyResponse();
        r.setFamilyId(f.getFamilyId());
        r.setFamilyName(f.getFamilyName());
        r.setDescription(f.getDescription());
        r.setPrivacySetting(f.getPrivacySetting());
        if (f.getParentFamily() != null) {
            r.setParentFamilyId(f.getParentFamily().getFamilyId());
            r.setParentFamilyName(f.getParentFamily().getFamilyName());
        }
        return r;
    }

    private Map<String, String> buildSpousePartnerMap(String familyId) {
        Map<String, String> map = new HashMap<>();
        for (Relationship r : relationshipRepository.findAllByFamilyId(familyId)) {
            if (!"SPOUSE".equals(r.getRelType())) {
                continue;
            }
            if (r.getPerson1() == null || r.getPerson2() == null) {
                continue;
            }
            String id1 = r.getPerson1().getUserId();
            String id2 = r.getPerson2().getUserId();
            map.putIfAbsent(id1, id2);
            map.putIfAbsent(id2, id1);
        }
        return map;
    }

    private void mapRequestToEntity(User user, UserRequest request) {
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setGender(request.getGender());
        user.setDob(request.getDob());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setHometown(request.getHometown());
        user.setCurrentAddress(request.getCurrentAddress());
        user.setOccupation(request.getOccupation());
        user.setBio(request.getBio());
        user.setGeneration(request.getGeneration());
        user.setOrderInFamily(request.getOrderInFamily());
        user.setParentId(request.getParentId());

        if (request.getFamilyId() != null) {
            Family fam = familyRepository.findById(request.getFamilyId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy dòng họ"));
            user.setFamily(fam);
        }
    }

    private void assertAssignableFamilyMemberRole(Role role) {
        if (role == null || role.getRoleName() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vai trò không hợp lệ.");
        }
        if (NON_ASSIGNABLE_FAMILY_UI_ROLE_NAMES.contains(role.getRoleName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Vai trò hệ thống (ADMIN) không gán qua màn trưởng họ.");
        }
        if (!ASSIGNABLE_MEMBER_ROLE_NAMES.contains(role.getRoleName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Vai trò này không được phép gán cho thành viên dòng họ.");
        }
    }

    private static boolean roleHasPermission(Role r, String permissionName) {
        if (r == null || r.getPermissions() == null) {
            return false;
        }
        for (Permission p : r.getPermissions()) {
            if (p != null && permissionName.equals(p.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param editingUserId null khi tạo mới; khi sửa — để chặn tự gỡ quyền quản trị thành viên.
     */
    private void applyMemberRole(User user, UserRequest request, String editingUserId, String managerUserId) {
        Role memberRole = roleRepository.findByRoleName("MEMBER")
                .orElseThrow(() -> new RuntimeException("Chưa cấu hình Role MEMBER trong DB!"));
        List<String> idList = request.getRoleIds();
        if (idList != null && idList.isEmpty() && editingUserId != null) {
            return;
        }
        if ((idList == null || idList.isEmpty()) && request.getRoleId() != null && !request.getRoleId().isBlank()) {
            idList = List.of(request.getRoleId().trim());
        }
        if (idList == null || idList.isEmpty()) {
            if (editingUserId == null) {
                user.setRoles(new LinkedHashSet<>(Set.of(memberRole)));
            }
            return;
        }
        Set<Role> next = new LinkedHashSet<>();
        for (String rid : idList) {
            if (rid == null || rid.isBlank()) {
                continue;
            }
            Role newRole = roleRepository.findById(rid.trim())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy vai trò."));
            if ("ADMIN".equals(newRole.getRoleName())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Role ADMIN chỉ do hệ thống cấp — không gán qua quản lý thành viên.");
            }
            assertAssignableFamilyMemberRole(newRole);
            next.add(newRole);
        }
        if (next.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chọn ít nhất một vai trò hợp lệ.");
        }
        if (editingUserId != null && editingUserId.equals(managerUserId)) {
            boolean had = UserRoleSupport.hasPermissionViaRoles(user, AppPermissions.MANAGE_FAMILY_MEMBERS);
            boolean will = next.stream().anyMatch(r -> roleHasPermission(r, AppPermissions.MANAGE_FAMILY_MEMBERS));
            if (had && !will) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Không thể gỡ quyền quản trị thành viên của chính mình.");
            }
        }
        user.setRoles(next);
    }

    private UserResponse mapToResponse(User user, Map<String, String> spouseByUserId) {
        UserResponse response = new UserResponse();
        response.setUserId(user.getUserId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setGender(user.getGender());
        response.setDob(user.getDob());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setHometown(user.getHometown());
        response.setCurrentAddress(user.getCurrentAddress());
        response.setOccupation(user.getOccupation());
        response.setBio(user.getBio());
        response.setAvatar(user.getAvatar());
        response.setParentId(user.getParentId());
        response.setGeneration(user.getGeneration());
        response.setOrderInFamily(user.getOrderInFamily());

        if (user.getFamily() != null) {
            response.setFamilyId(user.getFamily().getFamilyId());
            response.setFamilyName(user.getFamily().getFamilyName());
        }

        List<Role> sorted = UserRoleSupport.sortedRoles(user);
        if (!sorted.isEmpty()) {
            response.setRoleIds(sorted.stream().map(Role::getRoleId).collect(Collectors.toList()));
            response.setRoleNames(sorted.stream().map(Role::getRoleName).collect(Collectors.toList()));
            response.setRoleId(sorted.get(0).getRoleId());
            response.setRoleName(sorted.get(0).getRoleName());
            response.setPermissions(UserRoleSupport.mergedPermissionNames(user));
        }

        if (spouseByUserId != null && user.getUserId() != null) {
            response.setSpouseId(spouseByUserId.get(user.getUserId()));
        }

        return response;
    }
}
