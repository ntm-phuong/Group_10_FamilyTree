package com.family.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewsEventSummaryDTO {
    private String title;
    private LocalDateTime createdAt;
    private String status; // Ví dụ: "Đã đăng", "Lưu nháp"
}