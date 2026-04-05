package com.family.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Vai trò có thể gán cho thành viên trong phạm vi quản lý dòng họ. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberRoleOptionResponse {
    private String roleId;
    private String roleName;
    private String label;
}
