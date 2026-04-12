package com.family.app.controller;

import com.family.app.dto.FamilyTreeResponse;
import com.family.app.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Cho phép Frontend (React/Vue) gọi API mà không bị lỗi CORS
public class MemberController {

    private final MemberService memberService;

    /**
     * API lấy dữ liệu cấu trúc cây gia phả theo dòng họ.
     * Dùng chung cho cả Admin (để chỉnh sửa) và Client (để xem).
     *
     * @param familyId mã chi / gốc trong bảng {@code families}
     * @return DTO chứa danh sách nodes (thành viên) và links (quan hệ)
     */
    @GetMapping("/tree/{familyId}")
    public ResponseEntity<FamilyTreeResponse> getFamilyTree(@PathVariable String familyId) {
        try {
            FamilyTreeResponse treeData = memberService.getFamilyTreeData(familyId);
            return ResponseEntity.ok(treeData);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // POST /api/members (Thêm người mới)
    // PUT /api/members/{id} (Sửa thông tin)
    // DELETE /api/members/{id} (Xóa người)
}