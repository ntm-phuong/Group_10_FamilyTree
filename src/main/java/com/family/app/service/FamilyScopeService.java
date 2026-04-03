package com.family.app.service;

import com.family.app.model.Family;
import com.family.app.model.User;
import com.family.app.repository.FamilyRepository;
import com.family.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Phạm vi quản lý: trưởng họ có {@code MANAGE_FAMILY_NEWS} / {@code MANAGE_FAMILY_MEMBERS}
 * chỉ thao tác trên dòng họ của tài khoản và mọi chi con (đi xuống theo {@link Family#getParentFamily()}).
 */
@Service
public class FamilyScopeService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FamilyRepository familyRepository;

    /**
     * @return family_id gốc của tài khoản (bắt buộc đã gắn dòng họ)
     */
    @Transactional(readOnly = true)
    public String requireManagedFamilyId(String userId) {
        User u = userRepository.findByIdWithFamily(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng."));
        if (u.getFamily() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản chưa gắn dòng họ — không thể quản trị.");
        }
        return u.getFamily().getFamilyId();
    }

    /**
     * Mọi {@code family_id} mà user được phép quản lý: chi của họ + đệ quy mọi chi con.
     * Trưởng họ ({@code FAMILY_HEAD}): leo lên tổ tông (parent_family null) rồi gom toàn bộ chi con —
     * để quản trị được cả các chi phụ thuộc dưới một dòng họ gốc.
     */
    @Transactional(readOnly = true)
    public Set<String> manageableFamilyIds(String userId) {
        User u = userRepository.findByIdWithFamily(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng."));
        if (u.getFamily() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản chưa gắn dòng họ — không thể quản trị.");
        }
        String start = u.getFamily().getFamilyId();
        String roleName = u.getRole() != null && u.getRole().getRoleName() != null
                ? u.getRole().getRoleName()
                : "";
        String anchor = "FAMILY_HEAD".equals(roleName) ? climbToRootFamilyId(start) : start;
        return collectDescendantsFrom(anchor);
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

    public void assertCanManageFamily(String userId, String targetFamilyId) {
        if (targetFamilyId == null || targetFamilyId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu hoặc sai familyId.");
        }
        String t = targetFamilyId.trim();
        if (!manageableFamilyIds(userId).contains(t)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không được quản lý dòng họ này (ngoài phạm vi nhánh của bạn).");
        }
    }
}
