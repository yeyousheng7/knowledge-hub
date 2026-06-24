package com.yousheng.knowledgehub.ai.index.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecursiveTextSplitterTest {

    private static final List<String> DEFAULT_SEPARATORS = List.of("\n\n", "\n", "。", ".", "，", ",", " ", "");

    @Test
    void nullText_returnsEmptyList() {
        RecursiveTextSplitter splitter = new RecursiveTextSplitter(500, 50, DEFAULT_SEPARATORS);
        assertTrue(splitter.split(null).isEmpty());
    }

    @Test
    void emptyText_returnsEmptyList() {
        RecursiveTextSplitter splitter = new RecursiveTextSplitter(500, 50, DEFAULT_SEPARATORS);
        assertTrue(splitter.split("").isEmpty());
    }

    @Test
    void blankText_returnsEmptyList() {
        RecursiveTextSplitter splitter = new RecursiveTextSplitter(500, 50, DEFAULT_SEPARATORS);
        assertTrue(splitter.split("   \n  ").isEmpty());
    }

    @Test
    void shortText_returnsSingleChunk() {
        RecursiveTextSplitter splitter = new RecursiveTextSplitter(500, 50, DEFAULT_SEPARATORS);
        List<String> chunks = splitter.split("A short text.");
        assertEquals(1, chunks.size());
        assertEquals("A short text.", chunks.get(0));
    }

    @Test
    void longText_splitsIntoMultipleChunks() {
        RecursiveTextSplitter splitter = new RecursiveTextSplitter(100, 0, DEFAULT_SEPARATORS);
        String longText = "A".repeat(250);
        List<String> chunks = splitter.split(longText);
        assertTrue(chunks.size() > 1);
        for (String chunk : chunks) {
            assertTrue(chunk.length() <= 100, "chunk exceeds max size: " + chunk.length());
        }
    }

    @Test
    void chunkSizeNotPositive_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                new RecursiveTextSplitter(0, 0, DEFAULT_SEPARATORS));
        assertThrows(IllegalArgumentException.class, () ->
                new RecursiveTextSplitter(-1, 0, DEFAULT_SEPARATORS));
    }

    @Test
    void chunkOverlapNegative_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                new RecursiveTextSplitter(100, -1, DEFAULT_SEPARATORS));
    }

    @Test
    void chunkOverlapNotLessThanChunkSize_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                new RecursiveTextSplitter(100, 100, DEFAULT_SEPARATORS));
        assertThrows(IllegalArgumentException.class, () ->
                new RecursiveTextSplitter(100, 150, DEFAULT_SEPARATORS));
    }

    @Test
    void separatorsNull_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                new RecursiveTextSplitter(100, 0, null));
    }

    @Test
    void separatorsEmpty_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                new RecursiveTextSplitter(100, 0, List.of()));
    }

    // -- hard split must not double-overlap -------------------------------------------
    // The hard split (character-level) already produces overlapping pieces; mergeSplits
    // must not add a second layer of overlap on top.

    @Test
    void hardSplit_shortText_noDoubleOverlap() {
        RecursiveTextSplitter splitter = new RecursiveTextSplitter(10, 3, List.of(""));
        List<String> chunks = splitter.split("ABCDEFGHIJKLM");

        assertEquals(List.of("ABCDEFGHIJ", "HIJKLM"), chunks);
    }

    @Test
    void hardSplit_longText_noDoubleOverlap() {
        RecursiveTextSplitter splitter = new RecursiveTextSplitter(10, 3, List.of(""));
        List<String> chunks = splitter.split("abcdefghijklmnopqrstuvwxyz");

        assertEquals(List.of("abcdefghij", "hijklmnopq", "opqrstuvwx", "vwxyz"), chunks);
    }

    @Test
    void hardSplit_emojiUsesCodePointChunkSize() {
        RecursiveTextSplitter splitter = new RecursiveTextSplitter(2, 0, List.of(""));
        List<String> chunks = splitter.split("A😀B");

        assertEquals(List.of("A😀", "B"), chunks);
        for (String chunk : chunks) {
            assertTrue(chunk.codePointCount(0, chunk.length()) <= 2);
            assertFalse(hasUnpairedSurrogate(chunk));
        }
    }

    @Test
    void hardSplit_emojiOverlapDoesNotSplitSurrogatePair() {
        RecursiveTextSplitter splitter = new RecursiveTextSplitter(2, 1, List.of(""));
        List<String> chunks = splitter.split("A😀BC");

        assertEquals(List.of("A😀", "😀B", "BC"), chunks);
        for (String chunk : chunks) {
            assertTrue(chunk.codePointCount(0, chunk.length()) <= 2);
            assertFalse(hasUnpairedSurrogate(chunk));
        }
    }

    private boolean hasUnpairedSurrogate(String text) {
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (Character.isHighSurrogate(current)) {
                if (i + 1 >= text.length() || !Character.isLowSurrogate(text.charAt(i + 1))) {
                    return true;
                }
                i++;
                continue;
            }
            if (Character.isLowSurrogate(current)) {
                return true;
            }
        }
        return false;
    }
}
