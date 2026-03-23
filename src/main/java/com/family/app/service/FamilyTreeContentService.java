package com.family.app.service;

import com.family.app.model.FamilyMember;
import com.family.app.model.FamilyTreeStats;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class FamilyTreeContentService {

    private final List<FamilyMember> members = List.of(
        FamilyMember.builder().id(1L).name("Nguyen Van A").gender(FamilyMember.Gender.MALE).birthYear(1940).occupation("Truong toc").generation(1).build(),
        FamilyMember.builder().id(2L).name("Nguyen Thi B").gender(FamilyMember.Gender.FEMALE).birthYear(1945).occupation("Noi tro").generation(1).build(),
        FamilyMember.builder().id(3L).name("Nguyen Van C").gender(FamilyMember.Gender.MALE).birthYear(1968).occupation("Giao vien").generation(2).build(),
        FamilyMember.builder().id(4L).name("Nguyen Thi D").gender(FamilyMember.Gender.FEMALE).birthYear(1972).occupation("Ke toan").generation(2).build(),
        FamilyMember.builder().id(5L).name("Nguyen Van E").gender(FamilyMember.Gender.MALE).birthYear(1995).occupation("Ky su phan mem").generation(3).build(),
        FamilyMember.builder().id(6L).name("Nguyen Thi F").gender(FamilyMember.Gender.FEMALE).birthYear(1998).occupation("Bac si").generation(3).build()
    );

    public List<FamilyMember> findAllMembers() {
        return members.stream()
            .sorted(Comparator.comparingInt(FamilyMember::getGeneration).thenComparing(FamilyMember::getName))
            .toList();
    }

    public FamilyTreeStats buildStats() {
        int maxGeneration = members.stream().map(FamilyMember::getGeneration).max(Integer::compareTo).orElse(1);
        return FamilyTreeStats.builder()
            .totalMembers(members.size())
            .totalGenerations(maxGeneration)
            .build();
    }
}
