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
 * {@link NewsVisibility#FAMILY_ONLY} (chỉ khi người xem và chi gắn bài nằm trên cùng một đường
 * tổ tiên–hậu duệ: xem lên tổ tiên hoặc xuống con cháu; hai chi cùng cấp không xem tin nội bộ của nhau).
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

    /**
     * Tin nội bộ (FAMILY_ONLY) chỉ xem được khi chi của người xem và chi gắn bài nằm trên cùng một
     * đường dòng (một là tổ tiên của kia): con/cháu xem tin tổ tiên; cha/ông xem tin chi con; hai chi
     * cùng cấp (anh em) không xem được tin nội bộ của nhau.
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
        return findPublishedForNewsPage(q, category, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<NewsEvent> findPublishedForPublic(String q, NewsCategory category, String familyId) {
        return findPublishedForNewsPage(q, category, familyId, null, null);
    }

    @Transactional(readOnly = true)
    public List<NewsEvent> findPublishedForNewsPage(String q, NewsCategory category, String scopeFamilyId, User viewer) {
        return findPublishedForNewsPage(q, category, scopeFamilyId, viewer, null);
    }

    /**
     * @param scopeFamilyId lọc theo một chi (giống ?familyId=); null = không lọc theo chi.
     * @param viewer         null = khách; chỉ thấy PUBLIC_SITE. Có user = thêm FAMILY_ONLY nếu trong phạm vi nhánh.
     * @param visibilityFilter null = mọi loại; hoặc chỉ PUBLIC_SITE / FAMILY_ONLY.
     */
    @Transactional(readOnly = true)
    public List<NewsEvent> findPublishedForNewsPage(
            String q,
            NewsCategory category,
            String scopeFamilyId,
            User viewer,
            NewsVisibility visibilityFilter
    ) {
        List<NewsEvent> merged = new ArrayList<>();

        String scope = scopeFamilyId != null && !scopeFamilyId.isBlank() ? scopeFamilyId.trim() : null;

        if (scope != null) {
            merged.addAll(newsEventRepository.findByVisibilityAndFamilyFamilyIdOrderByCreatedAtDesc(
                    NewsVisibility.PUBLIC_SITE, scope));
        } else {
            merged.addAll(newsEventRepository.findByVisibilityOrderByCreatedAtDesc(NewsVisibility.PUBLIC_SITE));
        }

        if (viewer != null && viewer.getFamily() != null) {
            String vf = viewer.getFamily().getFamilyId();
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

    /** Query: {@code public} | {@code internal} — không phân biệt hoa thường. */
    public static NewsVisibility parseVisibilityFilter(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String v = raw.trim().toLowerCase(Locale.ROOT);
        if ("public".equals(v)) {
            return NewsVisibility.PUBLIC_SITE;
        }
        if ("internal".equals(v)) {
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
        List<NewsEvent> list = (familyId != null && !familyId.isBlank())
                ? newsEventRepository.findByVisibilityAndFeaturedTrueAndFamilyFamilyIdOrderByCreatedAtDesc(
                        NewsVisibility.PUBLIC_SITE, familyId.trim())
                : newsEventRepository.findByVisibilityAndFeaturedTrueOrderByCreatedAtDesc(NewsVisibility.PUBLIC_SITE);
        return list.stream().limit(limit).toList();
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
        return findRelated(article, limit, null, null);
    }

    @Transactional(readOnly = true)
    public List<NewsEvent> findRelated(NewsEvent article, int limit, String familyId) {
        return findRelated(article, limit, familyId, null);
    }

    @Transactional(readOnly = true)
    public List<NewsEvent> findRelated(NewsEvent article, int limit, String scopeFamilyId, User viewer) {
        if (limit <= 0) {
            return List.of();
        }
        boolean scoped = scopeFamilyId != null && !scopeFamilyId.isBlank();
        String fid = scoped ? scopeFamilyId.trim() : null;
        List<NewsEvent> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        seen.add(article.getId());

        if (article.getPublicCategory() != null) {
            List<NewsEvent> sameCat = scoped
                    ? newsEventRepository.findRelatedPublicForFamily(
                            NewsVisibility.PUBLIC_SITE, article.getPublicCategory(), article.getId(), fid)
                    : newsEventRepository.findRelatedPublic(
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
            List<NewsEvent> pool = findPublishedForNewsPage("", null, scopeFamilyId, viewer, null);
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
