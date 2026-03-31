package com.family.app.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NewsResponse {
    private String id;        // Đổi từ Long sang String
    private String title;
    private String summary;
    private String content;
    private String categoryId;
    private String categoryName;
    private String userId;
    private String userName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}