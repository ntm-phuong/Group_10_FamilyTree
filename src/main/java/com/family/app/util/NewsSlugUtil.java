package com.family.app.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Slug URL cho tin PUBLIC_SITE (unique toàn cục trong DB).
 */
public final class NewsSlugUtil {

    private NewsSlugUtil() {
    }

    public static String slugify(String title) {
        if (title == null || title.isBlank()) {
            return "bai-viet";
        }
        String n = Normalizer.normalize(title.trim(), Normalizer.Form.NFD);
        n = n.replaceAll("\\p{M}+", "");
        n = n.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        return n.isEmpty() ? "bai-viet" : n;
    }

    public static String uniqueSlug(String title, Predicate<String> slugExists) {
        String base = slugify(title);
        String candidate = base;
        int i = 2;
        while (slugExists.test(candidate)) {
            candidate = base + "-" + i;
            i++;
        }
        return candidate;
    }
}
