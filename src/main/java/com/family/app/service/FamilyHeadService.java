package com.family.app.service;

import com.family.app.dto.FamilyResponse;
import com.family.app.dto.FamilyWriteRequest;
import com.family.app.dto.UserRequest;
import com.family.app.dto.UserResponse;
import com.family.app.model.Family;
import com.family.app.model.Role;
import com.family.app.model.User;
import com.family.app.repository.FamilyRepository;
import com.family.app.repository.RoleRepository;
import com.family.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class FamilyHeadService {

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

    /** Chi của tài khoản và mọi chi con (cho dropdown / lọc). */
    @Transactional(readOnly = true)
    public List<FamilyResponse> listFamiliesInScope(String managerUserId) {
        return familyScopeService.manageableFamilyIds(managerUserId).stream()
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
        familyScopeService.assertCanManageFamily(managerUserId, id);
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
        familyScopeService.assertCanManageFamily(managerUserId, id);
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không thể xóa dòng họ từ tài khoản trưởng họ.");
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getMembersForManagedFamily(String familyId) {
        return userRepository.findByFamily_FamilyIdOrderByOrderInFamilyAsc(familyId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponse getMember(String id, String managerUserId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thành viên có ID: " + id));
        if (user.getFamily() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Thành viên không gắn dòng họ.");
        }
        familyScopeService.assertCanManageFamily(managerUserId, user.getFamily().getFamilyId());
        return mapToResponse(user);
    }

    @Transactional
    public UserResponse saveMember(UserRequest request, String managerUserId) {
        if (request.getFamilyId() == null || request.getFamilyId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu familyId.");
        }
        familyScopeService.assertCanManageFamily(managerUserId, request.getFamilyId().trim());

        String managedFamilyId = request.getFamilyId().trim();
        User user = new User();
        mapRequestToEntity(user, request);

        user.setPassword(passwordEncoder.encode("123456"));
        user.setStatus(1);

        if (request.getParentId() != null) {
            userRepository.findById(request.getParentId()).ifPresent(parent -> {
                if (parent.getFamily() == null
                        || !managedFamilyId.equals(parent.getFamily().getFamilyId())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cha/mẹ phải cùng dòng họ đang thêm.");
                }
                user.setGeneration(parent.getGeneration() + 1);
            });
        } else if (user.getGeneration() == null) {
            user.setGeneration(1);
        }

        Role memberRole = roleRepository.findByRoleName("MEMBER")
                .orElseThrow(() -> new RuntimeException("Chưa cấu hình Role MEMBER trong DB!"));
        user.setRole(memberRole);

        User savedUser = userRepository.save(user);
        return mapToResponse(savedUser);
    }

    @Transactional
    public UserResponse updateMember(String id, UserRequest request, String managerUserId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thành viên có ID: " + id));
        if (user.getFamily() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không thể sửa thành viên ngoài dòng họ.");
        }
        familyScopeService.assertCanManageFamily(managerUserId, user.getFamily().getFamilyId());
        if (request.getFamilyId() != null && !request.getFamilyId().isBlank()) {
            familyScopeService.assertCanManageFamily(managerUserId, request.getFamilyId().trim());
        }
        request.setFamilyId(request.getFamilyId() != null && !request.getFamilyId().isBlank()
                ? request.getFamilyId().trim()
                : user.getFamily().getFamilyId());
        mapRequestToEntity(user, request);

        User updatedUser = userRepository.save(user);
        return mapToResponse(updatedUser);
    }

    @Transactional
    public void deleteMember(String id, String managerUserId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Thành viên không tồn tại có ID: " + id));
        if (user.getFamily() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không thể xóa thành viên ngoài dòng họ.");
        }
        familyScopeService.assertCanManageFamily(managerUserId, user.getFamily().getFamilyId());

        boolean hasDescendants = userRepository.existsByParentId(id);
        if (hasDescendants) {
            throw new RuntimeException("Không thể xóa '" + user.getFullName()
                    + "' vì người này đã có con cháu. Vui lòng xóa hoặc cập nhật lại cha mẹ cho các con trước.");
        }

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

    private UserResponse mapToResponse(User user) {
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

        if (user.getFamily() != null) {
            response.setFamilyId(user.getFamily().getFamilyId());
            response.setFamilyName(user.getFamily().getFamilyName());
        }

        if (user.getRole() != null) {
            response.setRoleName(user.getRole().getRoleName());
        }

        return response;
    }
}
