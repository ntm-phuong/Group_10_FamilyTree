package com.family.app.service.kinship;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tiện ích đồ thị cha–con: khoảng cách lên tổ tiên và <strong>LCA</strong> (tổ tiên chung gần —
 * cực tiểu tổng hai khoảng cách).
 */
public final class BloodKinshipLcaAnalyzer {

    private BloodKinshipLcaAnalyzer() {
    }

    public static Map<String, Integer> collectAncestorDistance(
            String startId,
            Map<String, Set<String>> parentsByChild,
            int maxDepth
    ) {
        Map<String, Integer> dist = new HashMap<>();
        Set<String> frontier = new HashSet<>();
        frontier.add(startId);
        int depth = 0;
        while (!frontier.isEmpty() && depth < maxDepth) {
            depth++;
            Set<String> next = new HashSet<>();
            for (String node : frontier) {
                for (String p : parentsByChild.getOrDefault(node, Set.of())) {
                    dist.putIfAbsent(p, depth);
                    next.add(p);
                }
            }
            frontier = next;
        }
        return dist;
    }

    public static String findLcaId(
            Map<String, Integer> ancestorsOfViewer,
            Map<String, Integer> ancestorsOfOther
    ) {
        String best = null;
        int bestSum = Integer.MAX_VALUE;
        for (String u : ancestorsOfViewer.keySet()) {
            if (!ancestorsOfOther.containsKey(u)) {
                continue;
            }
            int sum = ancestorsOfViewer.get(u) + ancestorsOfOther.get(u);
            if (sum < bestSum || (sum == bestSum && best != null && u.compareTo(best) < 0)) {
                bestSum = sum;
                best = u;
            }
        }
        return best;
    }

    public static boolean areSiblings(
            String aId,
            String bId,
            Map<String, Set<String>> parentsByChild
    ) {
        Set<String> aParents = parentsByChild.getOrDefault(aId, Set.of());
        Set<String> bParents = parentsByChild.getOrDefault(bId, Set.of());
        return aParents.stream().anyMatch(bParents::contains);
    }
}
