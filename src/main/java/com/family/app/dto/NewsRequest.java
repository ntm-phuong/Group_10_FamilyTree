package com.family.app.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsRequest {
    private String title;
    private String summary;
    private String content;
    private String familyId;
    private String categoryId;
    private String authorId;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String location;
    private Integer remindBefore;
    private String visibility;
}