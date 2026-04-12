package com.family.app.controller;

import com.family.app.dto.HomeResponse;
import com.family.app.model.User;
import com.family.app.repository.UserRepository;
import com.family.app.service.FamilyScopeService;
import com.family.app.service.FamilyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/families")
@RequiredArgsConstructor
public class HomeController {

    private final FamilyService familyService;
    private final FamilyScopeService familyScopeService;
    private final UserRepository userRepository;

    /**
     * Thống kê trang chủ theo <strong>tổ tông</strong> của dòng họ người đăng nhập.
     * {@code id} có thể là bất kỳ {@code family_id} thuộc cùng cây; chỉ được phép nếu cùng gốc với user.
     */
    @GetMapping("/{id}/home")
    public ResponseEntity<HomeResponse> getHomeData(
            @PathVariable String id,
            @AuthenticationPrincipal User principal) {
        if (principal == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Cần đăng nhập.");
        }
        User u = userRepository.findByIdWithFamily(principal.getUserId())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Không tìm thấy người dùng."));
        if (u.getFamily() == null) {
            throw new ResponseStatusException(FORBIDDEN, "Tài khoản chưa gắn dòng họ.");
        }
        if (id == null || id.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Thiếu mã dòng họ.");
        }
        String userRoot = familyScopeService.resolveRootFamilyId(u.getFamily().getFamilyId());
        String requestedRoot = familyScopeService.resolveRootFamilyId(id.trim());
        if (!userRoot.equals(requestedRoot)) {
            throw new ResponseStatusException(FORBIDDEN, "Không có quyền xem dữ liệu dòng họ này.");
        }
        return ResponseEntity.ok(familyService.getHomeData(userRoot));
    }
}
