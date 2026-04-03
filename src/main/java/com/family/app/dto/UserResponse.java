package com.family.app.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class UserResponse {
    private String userId;
    private String fullName;
    private String email;
    private String gender;
    private LocalDate dob;
    private String phoneNumber;
    private String hometown;
    private String currentAddress;
    private String occupation;
    private String bio;
    private String avatar;
    /** ID dòng họ (FK) — tiện cho màn quản trị. */
    private String familyId;
    private String familyName;
    private String roleId;
    private String roleName;
    /** Tên quyền (GrantedAuthority) gắn với vai trò — tiện hiển thị. */
    private List<String> permissions;

    private String parentId;
    private Integer generation;
    private Integer orderInFamily;
    /** Vợ/chồng (từ quan hệ SPOUSE), nếu có. */
    private String spouseId;
}