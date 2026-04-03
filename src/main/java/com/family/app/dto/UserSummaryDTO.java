package com.family.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserSummaryDTO {
    private String fullName;
    private int generation;
    private String gender; // Để hiển thị màu sắc Nam (Xanh) / Nữ (Hồng)
}