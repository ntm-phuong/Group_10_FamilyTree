package com.family.app.service;

import com.family.app.dto.UserRequest;
import com.family.app.dto.UserResponse;
import com.family.app.model.Family;
import com.family.app.model.Role;
import com.family.app.model.User;
import com.family.app.repository.RoleRepository;
import com.family.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FamilyHeadService {

    @Autowired
    private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    // 1. Trưởng họ xem danh sách tất cả thành viên
    public List<UserResponse> getAllMembers() {
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // 2. Trưởng họ thêm thành viên mới (Tự động tạo tài khoản đăng nhập)
    @Transactional
    public UserResponse saveMember(UserRequest request) {
        User user = new User();
        mapRequestToEntity(user, request);

        // 1. Mã hóa mật khẩu (Bắt buộc để Spring Security check lúc login)
        // Nếu request không gửi password, ta để mặc định 123456
        user.setPassword(passwordEncoder.encode("123456"));

        // 2. Set trạng thái Active (1) để đăng nhập được ngay
        user.setStatus(1);

        // 3. Xử lý logic Thế hệ (Generation)
        // Nếu có parentId, thế hệ con = thế hệ cha + 1
        if (request.getParentId() != null) {
            userRepository.findById(request.getParentId()).ifPresent(parent -> {
                user.setGeneration(parent.getGeneration() + 1);
            });
        } else if (user.getGeneration() == null) {
            user.setGeneration(1); // Mặc định là đời thứ nhất nếu không rõ
        }

        // 4. Gán Role (Dùng findByRoleName từ Repo Phú vừa viết)
        Role memberRole = roleRepository.findByRoleName("MEMBER")
                .orElseThrow(() -> new RuntimeException("Chưa cấu hình Role MEMBER trong DB!"));
        user.setRole(memberRole);

        // 5. Lưu (Lúc này @PrePersist trong Entity sẽ tự sinh UUID cho userId)
        User savedUser = userRepository.save(user);

        return mapToResponse(savedUser);
    }
    // 3. Trưởng họ cập nhật thông tin thành viên
    @Transactional
    public UserResponse updateMember(String id, UserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thành viên có ID: " + id));

        mapRequestToEntity(user, request);

        User updatedUser = userRepository.save(user);
        return mapToResponse(updatedUser);
    }

    // 4. Trưởng họ xóa thành viên (Check ràng buộc con cái)
    @Transactional
    public void deleteMember(String id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Thành viên không tồn tại.");
        }

        // Kiểm tra xem ID này có đang là parentId của ai không
        boolean hasDescendants = userRepository.existsByParentId(id);
        if (hasDescendants) {
            throw new RuntimeException("Thành viên này đã có con cháu. Không thể xóa để tránh làm gãy cây gia phả!");
        }

        userRepository.deleteById(id);
    }

    public UserResponse getMemberById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Thành viên không tồn tại."));
        return mapToResponse(user);
    }

    // --- Helper Methods để xử lý Mapping (Sửa lỗi đỏ trong ảnh) ---

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

        // Sửa lỗi setFamilyId: Chuyển từ String sang Object Family
        if (request.getFamilyId() != null) {
            Family f = new Family();
            f.setFamilyId(request.getFamilyId());
            user.setFamily(f);
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

        // Sửa lỗi getFamilyId: Lấy ID từ Object Family
        if (user.getFamily() != null) {
            response.setFamilyName("Dòng họ ID: " + user.getFamily().getFamilyId());
        }

        // Sửa lỗi getRoleId: Lấy Name từ Object Role
        if (user.getRole() != null) {
            response.setRoleName(user.getRole().getRoleName());
        }

        return response;
    }
}