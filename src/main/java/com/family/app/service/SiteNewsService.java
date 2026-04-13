package com.family.app.service;

import com.family.app.model.Family;
import com.family.app.model.NewsCategory;
import com.family.app.model.NewsEvent;
import com.family.app.model.NewsVisibility;
import com.family.app.model.User;
import com.family.app.repository.FamilyRepository;
import com.family.app.repository.NewsEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Trang /news: hợp nhất tin {@link NewsVisibility#PUBLIC_SITE} (ai cũng xem được) và
 * {@link NewsVisibility#FAMILY_ONLY} (cùng nhánh dòng: tổ tiên / con cháu; không xem tin nội bộ chi anh em).
 */
@Service
public class SiteNewsService {

    private final NewsEventRepository newsEventRepository;
    private final FamilyRepository familyRepository;

    public SiteNewsService(NewsEventRepository newsEventRepository, FamilyRepository familyRepository) {
        this.newsEventRepository = newsEventRepository;
        this.familyRepository = familyRepository;
    }

    /**
     * Chuỗi family_id từ chi hiện tại lên gốc (gồm chính nó).
     */
    @Transactional(readOnly = true)
    public List<String> ancestorFamilyIds(String familyId) {
        List<String> ids = new ArrayList<>();
        if (familyId == null || familyId.isBlank()) {
            return ids;
        }
        String cur = familyId.trim();
        while (cur != null) {
            ids.add(cur);
            Optional<Family> fo = familyRepository.findByIdWithParentFamily(cur);
            if (fo.isEmpty()) {
                break;
            }
            Family p = fo.get().getParentFamily();
            cur = p != null ? p.getFamilyId() : null;
        }
        return ids;
    }

    /**
     * Tất cả {@code family_id} từ chi này xuống các chi con (BFS, gồm chính nó).
     */
    @Transactional(readOnly = true)
    public List<String> descendantFamilyIds(String familyId) {
        List<String> out = new ArrayList<>();
        if (familyId == null || familyId.isBlank()) {
            return out;
        }
        ArrayDeque<String> queue = new ArrayDeque<>();
        queue.add(familyId.trim());
        while (!queue.isEmpty()) {
            String id = queue.poll();
            out.add(id);
            for (Family ch : familyRepository.findByParentFamily_FamilyId(id)) {
                queue.add(ch.getFamilyId());
            }
        }
        return out;
    }

    private Set<String> subtreeFamilyIds(String familyId) {
        Set<String> ids = new HashSet<>(descendantFamilyIds(familyId));
        ids.add(familyId.trim());
        return ids;
    }

    /** Tổ tông (parent null) của một chi — cùng ý nghĩa với {@code FamilyScopeService#resolveRootFamilyId}. */
    private String climbToRootFamilyId(String familyId) {
        if (familyId == null || familyId.isBlank()) {
            return null;
        }
        String cur = familyId.trim();
        while (true) {
            Optional<Family> fo = familyRepository.findByIdWithParentFamily(cur);
            if (fo.isEmpty()) {
                break;
            }
            Family pf = fo.get().getParentFamily();
            if (pf == null || pf.getFamilyId() == null || pf.getFamilyId().isBlank()) {
                break;
            }
            cur = pf.getFamilyId().trim();
        }
        return cur;
    }

    /**
     * Tin nội bộ (FAMILY_ONLY): người xem và chi gắn bài nằm trên cùng một đường tổ tiên–hậu duệ
     * (con/cháu xem tin tổ/ông; cha xem tin chi con). Hai chi cùng cấp không xem tin nội bộ của nhau.
     */
    public boolean viewerMaySeeFamilyOnlyNews(User viewer, Family articleFamily) {
        if (viewer == null || viewer.getFamily() == null || articleFamily == null) {
            return false;
        }
        String vf = viewer.getFamily().getFamilyId();
        String af = articleFamily.getFamilyId();
        if (vf == null || af == null) {
            return false;
        }
        vf = vf.trim();
        af = af.trim();
        if (vf.equals(af)) {
            return true;
        }
        List<String> upFromViewer = ancestorFamilyIds(vf);
        List<String> upFromArticle = ancestorFamilyIds(af);
        return upFromViewer.contains(af) || upFromArticle.contains(vf);
    }

    public Optional<NewsEvent> findArticleForViewerBySlug(String slug, User viewer) {
        return newsEventRepository.findBySlugPublicOrFamily(slug)
                .filter(a -> canViewArticle(a, viewer));
    }

    /** @deprecated dùng {@link #findArticleForViewerBySlug(String, User)} */
    public Optional<NewsEvent> findPublicBySlug(String slug) {
        return findArticleForViewerBySlug(slug, null);
    }

    private boolean canViewArticle(NewsEvent a, User viewer) {
        if (a.getVisibility() == NewsVisibility.DRAFT) {
            return false;
        }
        if (a.getVisibility() == NewsVisibility.PUBLIC_SITE) {
            return true;
        }
        if (a.getVisibility() == NewsVisibility.FAMILY_ONLY) {
            return viewer != null && viewerMaySeeFamilyOnlyNews(viewer, a.getFamily());
        }
        return false;
    }

    @Transactional(readOnly = true)
    public List<NewsEvent> findPublishedForPublic(String q, NewsCategory category) {
        return findPublishedForNewsPage(q, category, null, null, null, false);
    }

    @Transactional(readOnly = true)
    public List<NewsEvent> findPublishedForPublic(String q, NewsCategory category, String familyId) {
        return findPublishedForNewsPage(q, category, familyId, null, null, false);
    }

    @Transactional(readOnly = true)
    public List<NewsEvent> findPublishedForNewsPage(String q, NewsCategory category, String scopeFamilyId, User viewer) {
        return findPublishedForNewsPage(q, category, scopeFamilyId, viewer, null, false);
    }

    /**
     * @param scopeFamilyId lọc theo tổ tông (hoặc chi — được chuẩn hóa ở controller); null = mọi PUBLIC_SITE toàn hệ thống.
     * @param viewer         null = khách; chỉ thấy PUBLIC_SITE. Có user = thêm FAMILY_ONLY trong phạm vi nhánh (khi không xem “dòng họ khác”).
     * @param visibilityFilter null = mọi loại; hoặc chỉ PUBLIC_SITE / FAMILY_ONLY.
     * @param externalScopePublicOnly true = đang xem dòng họ khác: chỉ tin {@link NewsVisibility#PUBLIC_SITE} trong cây {@code scope}, không gộp nội bộ của user.
     */
    @Transactional(readOnly = true)
    public List<NewsEvent> findPublishedForNewsPage(
            String q,
            NewsCategory category,
            String scopeFamilyId,
            User viewer,
            NewsVisibility visibilityFilter
    ) {
        return findPublishedForNewsPage(q, category, scopeFamilyId, viewer, visibilityFilter, false);
    }

    @Transactional(readOnly = true)
    public List<NewsEvent> findPublishedForNewsPage(
            String q,
            NewsCategory category,
            String scopeFamilyId,
            User viewer,
            NewsVisibility visibilityFilter,
            boolean externalScopePublicOnly
    ) {
        List<NewsEvent> merged = new ArrayList<>();

        String scope = scopeFamilyId != null && !scopeFamilyId.isBlank() ? scopeFamilyId.trim() : null;

        if (scope != null) {
            Set<String> scopeSubtree = subtreeFamilyIds(scope);
            merged.addAll(newsEventRepository.findByVisibilityAndFamilyFamilyIdIn(
                    NewsVisibility.PUBLIC_SITE, scopeSubtree));
        } else {
            merged.addAll(newsEventRepository.findByVisibilityOrderByCreatedAtDesc(NewsVisibility.PUBLIC_SITE));
        }

        if (!externalScopePublicOnly && viewer != null && viewer.getFamily() != null) {
            String vf = viewer.getFamily().getFamilyId();
            if (vf == null || vf.isBlank()) {
                vf = null;
            } else {
                vf = vf.trim();
            }
            if (vf != null) {
                Set<String> viewerReach = new HashSet<>(ancestorFamilyIds(vf));
                viewerReach.addAll(descendantFamilyIds(vf));

                List<String> allowedForInternal;
                if (scope != null) {
                    Set<String> scopeReach = new HashSet<>(ancestorFamilyIds(scope));
                    scopeReach.addAll(descendantFamilyIds(scope));
                    allowedForInternal = viewerReach.stream()
                            .filter(scopeReach::contains)
                            .toList();
                } else {
                    allowedForInternal = new ArrayList<>(viewerReach);
                }
                if (!allowedForInternal.isEmpty()) {
                    merged.addAll(newsEventRepository.findByVisibilityAndFamilyFamilyIdIn(
                            NewsVisibility.FAMILY_ONLY, allowedForInternal));
                }
            }
        }

        LinkedHashMap<String, NewsEvent> dedup = new LinkedHashMap<>();
        for (NewsEvent n : merged) {
            dedup.putIfAbsent(n.getId(), n);
        }
        List<NewsEvent> base = new ArrayList<>(dedup.values());
        base.sort(Comparator.comparing(NewsEvent::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        String query = q != null ? q.trim().toLowerCase(Locale.ROOT) : "";
        return base.stream()
                .filter(a -> a.getVisibility() != NewsVisibility.FAMILY_ONLY
                        || (viewer != null && viewerMaySeeFamilyOnlyNews(viewer, a.getFamily())))
                .filter(a -> visibilityFilter == null || a.getVisibility() == visibilityFilter)
                .filter(a -> category == null || a.getPublicCategory() == category)
                .filter(a -> query.isEmpty()
                        || a.getTitle().toLowerCase(Locale.ROOT).contains(query)
                        || (a.getSummary() != null && a.getSummary().toLowerCase(Locale.ROOT).contains(query))
                        || (a.getContent() != null && a.getContent().toLowerCase(Locale.ROOT).contains(query)))
                .toList();
    }

    /** Query: {@code PUBLIC_SITE} | {@code FAMILY_ONLY}; vẫn nhận alias cũ để tương thích ngược. */
    public static NewsVisibility parseVisibilityFilter(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String v = raw.trim().toLowerCase(Locale.ROOT);
        if ("public".equals(v) || "public_site".equals(v)) {
            return NewsVisibility.PUBLIC_SITE;
        }
        if ("internal".equals(v) || "family_only".equals(v)) {
            return NewsVisibility.FAMILY_ONLY;
        }
        return null;
    }

    @Transactional(readOnly = true)
    public List<NewsEvent> findFeaturedPublished(int limit) {
        return findFeaturedPublished(limit, null);
    }

    @Transactional(readOnly = true)
    public List<NewsEvent> findFeaturedPublished(int limit, String familyId) {
        if (familyId != null && !familyId.isBlank()) {
            return findFeaturedPublishedForScope(limit, familyId.trim());
        }
        List<NewsEvent> list = newsEventRepository.findByVisibilityAndFeaturedTrueOrderByCreatedAtDesc(NewsVisibility.PUBLIC_SITE);
        return list.stream().limit(limit).toList();
    }

    /** Tin nổi bật công khai thuộc cây (gốc + chi con) của {@code scopeRootFamilyId}. */
    @Transactional(readOnly = true)
    public List<NewsEvent> findFeaturedPublishedForScope(int limit, String scopeRootFamilyId) {
        if (limit <= 0 || scopeRootFamilyId == null || scopeRootFamilyId.isBlank()) {
            return List.of();
        }
        Set<String> ids = subtreeFamilyIds(scopeRootFamilyId.trim());
        List<NewsEvent> pool = newsEventRepository.findByVisibilityAndFamilyFamilyIdIn(NewsVisibility.PUBLIC_SITE, ids);
        return pool.stream()
                .filter(NewsEvent::isFeatured)
                .sorted(Comparator.comparing(NewsEvent::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .toList();
    }

    @Transactional
    public void incrementViewCount(String id) {
        newsEventRepository.findById(id).ifPresent(a -> {
            a.setViewCount(a.getViewCount() + 1);
            newsEventRepository.save(a);
        });
    }

    @Transactional(readOnly = true)
    public List<NewsEvent> findRelated(NewsEvent article, int limit) {
        return findRelated(article, limit, (User) null);
    }

    @Transactional(readOnly = true)
    public List<NewsEvent> findRelated(NewsEvent article, int limit, User viewer) {
        if (limit <= 0) {
            return List.of();
        }
        String articleFid = article.getFamily() != null ? article.getFamily().getFamilyId() : null;
        String scopeRoot = (articleFid != null && !articleFid.isBlank()) ? climbToRootFamilyId(articleFid.trim()) : null;
        String viewerRoot = (viewer != null && viewer.getFamily() != null && viewer.getFamily().getFamilyId() != null)
                ? climbToRootFamilyId(viewer.getFamily().getFamilyId().trim())
                : null;
        boolean externalScopePublicOnly = scopeRoot == null
                || viewerRoot == null
                || !scopeRoot.equals(viewerRoot);

        Set<String> scopeSubtree = scopeRoot != null ? subtreeFamilyIds(scopeRoot) : null;
        List<NewsEvent> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        seen.add(article.getId());

        if (article.getPublicCategory() != null && scopeSubtree != null && !scopeSubtree.isEmpty()) {
            List<NewsEvent> sameCat = newsEventRepository.findRelatedPublicForFamilies(
                    NewsVisibility.PUBLIC_SITE, article.getPublicCategory(), article.getId(), scopeSubtree);
            for (NewsEvent n : sameCat) {
                if (out.size() >= limit) {
                    break;
                }
                if (seen.add(n.getId())) {
                    out.add(n);
                }
            }
        } else if (article.getPublicCategory() != null) {
            List<NewsEvent> sameCat = newsEventRepository.findRelatedPublic(
                    NewsVisibility.PUBLIC_SITE, article.getPublicCategory(), article.getId());
            for (NewsEvent n : sameCat) {
                if (out.size() >= limit) {
                    break;
                }
                if (seen.add(n.getId())) {
                    out.add(n);
                }
            }
        }
        if (out.size() < limit) {
            List<NewsEvent> pool = findPublishedForNewsPage(
                    "", null, scopeRoot, viewer, null, externalScopePublicOnly);
            for (NewsEvent n : pool) {
                if (out.size() >= limit) {
                    break;
                }
                if (seen.add(n.getId())) {
                    out.add(n);
                }
            }
        }
        return out;
    }

    /** @deprecated dùng {@link #findRelated(NewsEvent, int, User)} */
    @Deprecated
    @Transactional(readOnly = true)
    public List<NewsEvent> findRelated(NewsEvent article, int limit, String scopeFamilyId, User viewer) {
        return findRelated(article, limit, viewer);
    }

    public static NewsCategory parseCategory(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return NewsCategory.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
