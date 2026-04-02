package com.family.app.controller;

import com.family.app.model.Relationship;
import com.family.app.model.User;
import com.family.app.repository.RelationshipRepository;
import com.family.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/member")
@RequiredArgsConstructor
public class PublicMemberController {

    private final UserRepository userRepository;
    private final RelationshipRepository relationshipRepository;

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public String memberDetail(@PathVariable String id, Model model) {
        User member = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Thành viên không tồn tại: " + id));

        User spouse = findSpouse(member.getUserId());
        List<User> parents = findParents(member.getUserId());
        User father = parents.stream().filter(p -> "MALE".equalsIgnoreCase(p.getGender())).findFirst().orElse(null);
        User mother = parents.stream().filter(p -> "FEMALE".equalsIgnoreCase(p.getGender())).findFirst().orElse(null);

        List<User> children = findChildren(member.getUserId());
        List<User> siblings = findSiblings(member.getUserId(), parents);

        model.addAttribute("member", member);
        model.addAttribute("memberAge", calculateAge(member.getDob(), member.getDod()));
        model.addAttribute("father", father);
        model.addAttribute("mother", mother);
        model.addAttribute("spouse", spouse);
        model.addAttribute("children", children);
        model.addAttribute("siblings", siblings);
        model.addAttribute("activeMenu", "family-tree");
        return "public/member-detail";
    }

    private User findSpouse(String userId) {
        List<Relationship> spouseRelations = relationshipRepository.findSpouses(userId);
        if (spouseRelations.isEmpty()) {
            return null;
        }
        Relationship relation = spouseRelations.get(0);
        User other = null;
        if (relation.getPerson1() != null && userId.equals(relation.getPerson1().getUserId())) {
            other = relation.getPerson2();
        } else {
            other = relation.getPerson1();
        }
        return materializeUser(other);
    }

    /** Load a fresh managed row so fields are available when the view renders (open-in-view is off). */
    private User materializeUser(User ref) {
        if (ref == null) {
            return null;
        }
        return userRepository.findById(ref.getUserId()).orElse(null);
    }

    private List<User> findParents(String userId) {
        List<Relationship> parentRelations = relationshipRepository.findByPerson2_UserIdAndRelType(userId, "PARENT_CHILD");
        List<User> parents = new ArrayList<>();
        for (Relationship relation : parentRelations) {
            User p = materializeUser(relation.getPerson1());
            if (p != null) {
                parents.add(p);
            }
        }
        return parents;
    }

    private List<User> findChildren(String userId) {
        List<Relationship> childRelations = relationshipRepository.findByPerson1_UserIdAndRelType(userId, "PARENT_CHILD");
        Map<String, User> unique = new LinkedHashMap<>();
        for (Relationship relation : childRelations) {
            User child = materializeUser(relation.getPerson2());
            if (child != null) {
                unique.put(child.getUserId(), child);
            }
        }
        return unique.values().stream()
                .sorted(Comparator.comparing(User::getDob, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private List<User> findSiblings(String userId, List<User> parents) {
        Set<User> siblings = new LinkedHashSet<>();
        for (User parent : parents) {
            List<Relationship> childRelations = relationshipRepository.findByPerson1_UserIdAndRelType(parent.getUserId(), "PARENT_CHILD");
            for (Relationship relation : childRelations) {
                User sibling = materializeUser(relation.getPerson2());
                if (sibling != null && !userId.equals(sibling.getUserId())) {
                    siblings.add(sibling);
                }
            }
        }
        return siblings.stream()
                .sorted(Comparator.comparing(User::getDob, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private Integer calculateAge(LocalDate dob, LocalDate dod) {
        if (dob == null) {
            return null;
        }
        LocalDate endDate = dod != null ? dod : LocalDate.now();
        return Period.between(dob, endDate).getYears();
    }
}
