package com.family.app.service.kinship;

/**
 * Kết quả phân tích quan hệ máu: mã + tham số (khoảng cách / lệch thế hệ) + tổ tiên chung gần (LCA) nếu có.
 *
 * @param lcaId đỉnh LCA (theo cha–con) khi dùng cho nhánh bàng hệ; có thể {@code null}.
 */
public record BloodKinshipStructure(BloodKinshipKind kind, int distanceOrGenGap, String lcaId) {
    public static BloodKinshipStructure of(BloodKinshipKind kind, int distanceOrGenGap) {
        return new BloodKinshipStructure(kind, distanceOrGenGap, null);
    }

    public static BloodKinshipStructure of(BloodKinshipKind kind, int distanceOrGenGap, String lcaId) {
        return new BloodKinshipStructure(kind, distanceOrGenGap, lcaId);
    }
}
