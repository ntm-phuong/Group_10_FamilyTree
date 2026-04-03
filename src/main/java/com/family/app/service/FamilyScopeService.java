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
     */
    @Transactional(readOnly = true)
    public Set<String> manageableFamilyIds(String userId) {
        String root = requireManagedFamilyId(userId);
        Set<String> ids = new LinkedHashSet<>();
        ArrayDeque<String> q = new ArrayDeque<>();
        q.add(root);
        ids.add(root);
        while (!q.isEmpty()) {
            String id = q.poll();
            List<Family> children = familyRepository.findByParentFamily_FamilyId(id);
            for (Family c : children) {
                if (ids.add(c.getFamilyId())) {
                    q.add(c.getFamilyId());
                }
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
