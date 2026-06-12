package com.yousheng.knowledgehub.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SqlLikeUtilsTest {

    @Test
    void null_returnsNull() {
        assertNull(SqlLikeUtils.toContainsPattern(null));
    }

    @Test
    void empty_returnsNull() {
        assertNull(SqlLikeUtils.toContainsPattern(""));
    }

    @Test
    void blank_returnsNull() {
        assertNull(SqlLikeUtils.toContainsPattern("   "));
    }

    @Test
    void normalKeyword_returnsLowercaseWithWildcards() {
        assertEquals("%spring%", SqlLikeUtils.toContainsPattern("Spring"));
    }

    @Test
    void percent_escaped() {
        assertEquals("%100!%%", SqlLikeUtils.toContainsPattern("100%"));
    }

    @Test
    void underscore_escaped() {
        assertEquals("%under!_score%", SqlLikeUtils.toContainsPattern("under_score"));
    }

    @Test
    void exclamation_escaped() {
        assertEquals("%wow!!%", SqlLikeUtils.toContainsPattern("wow!"));
    }
}
