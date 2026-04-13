package com.family.app.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
public class FamilyTreeResponse {
    private String familyName;
    /** Đồng bộ {@link com.family.app.dto.ClanMemberStatistics#totalGenerations()} — max đời trong DB, 0 nếu không có. */
    private Integer totalGenerations;
    /** Cùng nguồn với trang /home (đếm theo {@code users.family_id} trong phạm vi cây). */
    private Long totalMembers;
    private Long maleCount;
    private Long femaleCount;
    private List<MemberNode> members;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberNode {
        private String user_id;
        private String name;
        private String gender;
        private Integer birthYear;
        private Integer deathYear;
        private String occupation;
        private Integer generation;
        private String branch;
        private Integer orderInFamily;
        private String avatar;

        // Các trường quan hệ để FE vẽ cây
        /** Cha/mẹ trực tiếp (cột user.parent_id) — dùng khi chưa có bản ghi Relationship PARENT_CHILD. */
        private String parentId;
        private String fatherId;
        private String motherId;
        private String spouseId;

        private boolean isDead;
    }
}