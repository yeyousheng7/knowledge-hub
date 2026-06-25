package com.yousheng.knowledgehub.ai.tool.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiToolPageTest {

    @Test
    void normalPagination_constructsCorrectly() {
        List<String> items = List.of("a", "b", "c");
        AiToolPage<String> page = new AiToolPage<>(1, 5, 3, true, items);

        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(5);
        assertThat(page.returned()).isEqualTo(3);
        assertThat(page.hasMore()).isTrue();
        assertThat(page.items()).containsExactly("a", "b", "c");
    }

    @Test
    void emptyList_defaultsItemsToEmpty() {
        AiToolPage<String> page = new AiToolPage<>(1, 5, 0, false, null);

        assertThat(page.items()).isEmpty();
        assertThat(page.returned()).isZero();
        assertThat(page.hasMore()).isFalse();
    }

    @Test
    void returnedMatchesItemsSize() {
        List<String> items = List.of("x", "y");
        AiToolPage<String> page = new AiToolPage<>(2, 10, 2, false, items);

        assertThat(page.returned()).isEqualTo(page.items().size());
        assertThat(page.returned()).isEqualTo(2);
    }

    @Test
    void lastPage_hasMoreFalse() {
        List<String> items = List.of("last");
        AiToolPage<String> page = new AiToolPage<>(3, 2, 1, false, items);

        assertThat(page.returned()).isEqualTo(1);
        assertThat(page.hasMore()).isFalse();
    }

    @Test
    void directConstruct_mutableItems_doesNotAffectPage() {
        ArrayList<String> mutable = new ArrayList<>(List.of("a", "b"));

        AiToolPage<String> page = new AiToolPage<>(1, 5, 2, false, mutable);

        mutable.add("c");

        assertThat(page.items()).containsExactly("a", "b");
    }

    @Test
    void itemsReturnedList_isUnmodifiable() {
        AiToolPage<String> page = new AiToolPage<>(1, 5, 1, false, List.of("x"));

        assertThatThrownBy(() -> page.items().add("y"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
