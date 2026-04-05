package com.family.app.security;

import com.family.app.model.Permission;
import com.family.app.model.Role;
import com.family.app.model.User;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Hỗ trợ user có nhiều {@link Role}; gộp quyền và kiểm tra vai trò.
 */
public final class UserRoleSupport {

    private UserRoleSupport() {
    }

    public static boolean hasRoleName(User user, String roleName) {
        if (user == null || user.getRoles() == null || roleName == null) {
            return false;
        }
        for (Role r : user.getRoles()) {
            if (r != null && roleName.equals(r.getRoleName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasRoleId(User user, String roleId) {
        if (user == null || user.getRoles() == null || roleId == null) {
            return false;
        }
        for (Role r : user.getRoles()) {
            if (r != null && roleId.equals(r.getRoleId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Quản trị toàn cây phả hệ (mọi chi dưới tổ tông app): quyền {@link AppPermissions#MANAGE_CLAN}
     * hoặc vai trò {@code ADMIN} (trong app, ADMIN luôn gắn MANAGE_CLAN; kiểm tra tên role là lớp dự phòng khi hydrate permissions lỗi).
     */
    public static boolean hasClanWideAdminAccess(User user) {
        return hasRoleName(user, "ADMIN")
                || hasPermissionViaRoles(user, AppPermissions.MANAGE_CLAN);
    }

    public static boolean hasPermissionViaRoles(User user, String permissionName) {
        if (user == null || user.getRoles() == null || permissionName == null) {
            return false;
        }
        for (Role r : user.getRoles()) {
            if (r == null || r.getPermissions() == null) {
                continue;
            }
            for (Permission p : r.getPermissions()) {
                if (p != null && permissionName.equals(p.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static List<String> mergedPermissionNames(User user) {
        Set<String> names = new LinkedHashSet<>();
        if (user != null && user.getRoles() != null) {
            for (Role r : user.getRoles()) {
                if (r == null || r.getPermissions() == null) {
                    continue;
                }
                for (Permission p : r.getPermissions()) {
                    if (p != null && p.getName() != null && !p.getName().isBlank()) {
                        names.add(p.getName().trim());
                    }
                }
            }
        }
        return names.stream().sorted().collect(Collectors.toList());
    }

    public static List<String> roleNames(User user) {
        if (user == null || user.getRoles() == null || user.getRoles().isEmpty()) {
            return List.of();
        }
        return user.getRoles().stream()
                .filter(Objects::nonNull)
                .map(Role::getRoleName)
                .filter(Objects::nonNull)
                .sorted()
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Một chuỗi vai trò “ưu tiên” cho FE cũ (localStorage.role) — không thay thế danh sách đầy đủ.
     */
    public static String primaryRoleNameForFe(User user) {
        List<String> names = roleNames(user);
        if (names.isEmpty()) {
            return null;
        }
        if (names.contains("ADMIN")) {
            return "ADMIN";
        }
        if (names.contains("FAMILY_HEAD")) {
            return "FAMILY_HEAD";
        }
        if (names.contains("FAMILY_NEWS_MANAGER")) {
            return "FAMILY_NEWS_MANAGER";
        }
        return names.get(0);
    }

    public static List<Role> sortedRoles(User user) {
        if (user == null || user.getRoles() == null || user.getRoles().isEmpty()) {
            return List.of();
        }
        List<Role> list = new ArrayList<>(user.getRoles());
        list.sort(Comparator.comparing(r -> r.getRoleName() != null ? r.getRoleName() : "", Comparator.nullsLast(String::compareTo)));
        return list;
    }
}
