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
    private LocalDateTime endAt;
    private String location;
    private Integer remindBefore;

    private String categoryId;
    private String categoryName;
    private String userId;
    private String userName;

    private String visibility;
    private String slug;
    private Boolean featured;
    private Integer viewCount;
    private String coverImage;
    private String publicCategory;

    private String familyId;
    private String familyName;
}