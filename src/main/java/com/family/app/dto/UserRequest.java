package com.family.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    private String fullName;

    @Email(message = "Email không đúng định dạng")
    private String email;

    private String gender;

    @Past(message = "Ngày sinh phải là một ngày trong quá khứ")
    private LocalDate dob;

    @Pattern(regexp = "(^$|[0-9]{10})", message = "Số điện thoại phải có 10 chữ số")
    private String phoneNumber;

    private String hometown;
    private String currentAddress;
    private String occupation;
    private String bio;

    @NotBlank(message = "Phải xác định dòng họ")
    private String familyId;

    /** Một hoặc nhiều vai trò (ưu tiên dùng khi gán nhiều quyền). */
    private List<String> roleIds;

    /** Tương thích cũ: một roleId — map sang {@code roleIds} nếu list rỗng. */
    private String roleId;

    // parentId có thể null nếu là người đời thứ nhất (Tổ tiên)
    private String parentId;

    private Integer generation;
    private Integer orderInFamily;
}