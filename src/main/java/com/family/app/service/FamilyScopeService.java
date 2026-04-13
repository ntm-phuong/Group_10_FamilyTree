package com.family.app.service;

import com.family.app.config.AppClanProperties;
import com.family.app.model.Family;
import com.family.app.model.User;
import com.family.app.repository.FamilyRepository;
import com.family.app.repository.UserRepository;
import com.family.app.security.AppPermissions;
import com.family.app.security.UserRoleSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Phạm vi quản lý theo dòng họ:
 * <ul>
 *   <li><b>Thành viên / chi</b>: nhánh từ {@code user.family} xuống (không sang nhánh anh em).</li>
 *   <li><b>Tin</b>: cùng nhánh; có {@code FAMILY_NEWS_MANAGER} thì leo lên tổ tông rồi toàn bộ chi con.</li>
 *   <li><b>Toàn cây</b> ({@link AppPermissions#MANAGE_CLAN} hoặc role {@code ADMIN}): mọi chi từ {@code app.clan.family-id}.</li>
 * </ul>
 */
@Service
public class FamilyScopeService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FamilyRepository familyRepository;

    @Autowired
    private AppClanProperties clanProperties;

    @Transactional(readOnly = true)
    public String requireManagedFamilyId(String userId) {
        User u = userRepository.findByIdWithFamily(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng."));
        if (UserRoleSupport.hasClanWideAdminAccess(u)) {
            if (u.getFamily() != null) {
                return climbToRootFamilyId(u.getFamily().getFamilyId());
            }
            return configuredClanRootOrThrow();
        }
        if (u.getFamily() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản chưa gắn dòng họ — không thể quản trị.");
        }
        return u.getFamily().getFamilyId();
    }

    /**
     * Tổ tông (bản ghi không có parent_family_id) của một chi — dùng SSR gia phả / mặc định theo user.
     */
    @Transactional(readOnly = true)
    public String resolveRootFamilyId(String familyId) {
        if (familyId == null || familyId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu familyId.");
        }
        return climbToRootFamilyId(familyId.trim());
    }

    /**
     * Phạm vi quản lý <strong>thành viên và cấu trúc chi</strong> — chỉ nhánh từ {@code user.family} đi xuống.
     */
    @Transactional(readOnly = true)
    public Set<String> manageableFamilyIdsForMembers(String userId) {
        User u = loadUserForScope(userId);
        if (UserRoleSupport.hasClanWideAdminAccess(u)) {
            return clanWideManageableSubtreeIds(u);
        }
        String start = u.getFamily().getFamilyId();
        return collectDescendantsFrom(start);
    }

    /**
     * Dashboard trưởng họ — thống kê & tin gần đây: toàn bộ chi dưới <strong>tổ tông</strong> của user.
     * Tin trong DB thường gắn {@code family_id} ở dòng họ gốc; không dùng {@link #manageableFamilyIdsForMembers}
     * (chỉ nhánh xuống) để tránh đếm 0 khi user thuộc chi con.
     */
    @Transactional(readOnly = true)
    public Set<String> manageableFamilyIdsForHeadDashboardNews(String userId) {
        User u = loadUserForScope(userId);
        if (UserRoleSupport.hasClanWideAdminAccess(u)) {
            return clanWideManageableSubtreeIds(u);
        }
        String root = climbToRootFamilyId(u.getFamily().getFamilyId());
        return collectDescendantsFrom(root);
    }

    /**
     * Phạm vi quản lý <strong>tin nội bộ dòng họ</strong> — có {@code FAMILY_NEWS_MANAGER} thì mở rộng theo tổ tông.
     */
    @Transactional(readOnly = true)
    public Set<String> manageableFamilyIdsForNews(String userId) {
        User u = loadUserForScope(userId);
        if (UserRoleSupport.hasClanWideAdminAccess(u)) {
            return clanWideManageableSubtreeIds(u);
        }
        String start = u.getFamily().getFamilyId();
        String anchor = UserRoleSupport.hasRoleName(u, "FAMILY_NEWS_MANAGER")
                ? climbToRootFamilyId(start)
                : start;
        return collectDescendantsFrom(anchor);
    }

    /** ADMIN / MANAGE_CLAN: cây dưới tổ tông của chính user; nếu chưa gắn dòng họ thì fallback cấu hình app (tài khoản hệ thống). */
    private Set<String> clanWideManageableSubtreeIds(User u) {
        if (u.getFamily() != null) {
            String root = climbToRootFamilyId(u.getFamily().getFamilyId());
            return collectDescendantsFrom(root);
        }
        return collectDescendantsFrom(configuredClanRootOrThrow());
    }

    private String configuredClanRootOrThrow() {
        String root = clanProperties.getFamilyId();
        if (root == null || root.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Chưa cấu hình app.clan.family-id.");
        }
        return root.trim();
    }

    private User loadUserForScope(String userId) {
        User u = userRepository.findByIdWithFamily(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng."));
        if (UserRoleSupport.hasClanWideAdminAccess(u)) {
            return u;
        }
        if (u.getFamily() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản chưa gắn dòng họ — không thể quản trị.");
        }
        return u;
    }

    public void assertCanManageFamilyMembers(String userId, String targetFamilyId) {
        assertInSet(targetFamilyId, manageableFamilyIdsForMembers(userId), "Không được quản lý dòng họ này (ngoài phạm vi nhánh của bạn).");
    }

    public void assertCanManageFamilyNews(String userId, String targetFamilyId) {
        assertInSet(targetFamilyId, manageableFamilyIdsForNews(userId), "Không được quản lý tin của dòng họ này (ngoài phạm vi được phép).");
    }

    private static void assertInSet(String targetFamilyId, Set<String> allowed, String message) {
        if (targetFamilyId == null || targetFamilyId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu hoặc sai familyId.");
        }
        String t = targetFamilyId.trim();
        if (!allowed.contains(t)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
    }

    private String climbToRootFamilyId(String familyId) {
        String current = familyId;
        for (int i = 0; i < 64; i++) {
            Family f = familyRepository.findByIdWithParentFamily(current).orElse(null);
            if (f == null || f.getParentFamily() == null) {
                return current;
            }
            current = f.getParentFamily().getFamilyId();
        }
        return current;
    }

    private Set<String> collectDescendantsFrom(String rootId) {
        Set<String> ids = new LinkedHashSet<>();
        ArrayDeque<String> q = new ArrayDeque<>();
        q.add(rootId);
        while (!q.isEmpty()) {
            String id = q.poll();
            if (!ids.add(id)) {
                continue;
            }
            for (Family c : familyRepository.findByParentFamily_FamilyId(id)) {
                q.add(c.getFamilyId());
            }
        }
        return ids;
    }
}
