package com.yousheng.knowledgehub.common.util;

import java.util.Locale;

public final class SqlLikeUtils {

    public static String toContainsPattern(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return null;
        }

        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        if (normalizedKeyword.isEmpty()) {
            return null;
        }

        String escaped = normalizedKeyword
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");

        return "%" + escaped + "%";
    }

    private SqlLikeUtils() {
    }
}
