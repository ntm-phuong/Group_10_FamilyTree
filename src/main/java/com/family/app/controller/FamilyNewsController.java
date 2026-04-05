package com.family.app.controller;

import com.family.app.dto.NewsRequest;
import com.family.app.dto.NewsResponse;
import com.family.app.model.User;
import com.family.app.service.FamilyScopeService;
import com.family.app.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/family-head/news")
@PreAuthorize(
        "hasAnyAuthority('MANAGE_FAMILY_NEWS','MANAGE_CLAN','FAMILY_HEAD','ROLE_FAMILY_NEWS_MANAGER','ROLE_FAMILY_BRANCH_MANAGER')")
public class FamilyNewsController {

    @Autowired
    private NewsService newsService;
    @Autowired
    private FamilyScopeService familyScopeService;

    @GetMapping("/family/{familyId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<NewsResponse>> getByFamily(
            @PathVariable String familyId,
            @AuthenticationPrincipal User principal) {
        familyScopeService.assertCanManageFamilyNews(principal.getUserId(), familyId);
        return ResponseEntity.ok(newsService.getNewsByFamily(familyId));
    }

    @PostMapping
    public ResponseEntity<NewsResponse> create(
            @RequestBody NewsRequest request,
            @AuthenticationPrincipal User principal) {
        if (request.getFamilyId() == null || request.getFamilyId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu familyId.");
        }
        familyScopeService.assertCanManageFamilyNews(principal.getUserId(), request.getFamilyId().trim());
        return ResponseEntity.ok(newsService.createNews(request));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<NewsResponse> update(
            @PathVariable String id,
            @RequestBody NewsRequest request,
            @AuthenticationPrincipal User principal) {
        NewsResponse existing = newsService.getNewsById(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tin.");
        }
        if (existing.getFamilyId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tin không gắn dòng họ.");
        }
        familyScopeService.assertCanManageFamilyNews(principal.getUserId(), existing.getFamilyId());
        if (request.getFamilyId() != null && !request.getFamilyId().isBlank()) {
            familyScopeService.assertCanManageFamilyNews(principal.getUserId(), request.getFamilyId().trim());
        }
        if (request.getFamilyId() == null || request.getFamilyId().isBlank()) {
            request.setFamilyId(existing.getFamilyId());
        }
        return ResponseEntity.ok(newsService.updateNews(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable String id, @AuthenticationPrincipal User principal) {
        NewsResponse existing = newsService.getNewsById(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tin.");
        }
        if (existing.getFamilyId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không xóa được tin này.");
        }
        familyScopeService.assertCanManageFamilyNews(principal.getUserId(), existing.getFamilyId());
        newsService.deleteNews(id);
        return ResponseEntity.ok("Xóa tin tức thành công.");
    }
}
