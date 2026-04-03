package com.family.app.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NewsResponse {
    private String id;
    private String title;
    private String summary;
    private String content;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime startAt;

    private String categoryId;
    private String categoryName;
    private String userId;
    private String userName;
}