package com.family.app.dto;

/**
 * Thống kê thành viên trên một tập {@code family_id} (gốc + chi con),
 * dùng chung cho {@link com.family.app.service.MemberService#getFamilyTreeData},
 * {@link com.family.app.service.FamilyService#getHomeData} và dashboard.
 */
public record ClanMemberStatistics(
        long totalMembers,
        long maleCount,
        long femaleCount,
        int totalGenerations
) {
    public static ClanMemberStatistics empty() {
        return new ClanMemberStatistics(0L, 0L, 0L, 0);
    }
}
