package com.yousheng.knowledgehub.ai.index.util;

import java.util.ArrayList;
import java.util.List;

public class RecursiveTextSplitter {
    private final int chunkSize;
    private final int chunkOverlap;
    private final List<String> separators;

    public RecursiveTextSplitter(int chunkSize, int chunkOverlap, List<String> separators) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be positive");
        }
        if (chunkOverlap < 0) {
            throw new IllegalArgumentException("chunkOverlap must not be negative");
        }
        if (chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException("chunkOverlap must be smaller than chunkSize");
        }
        if (separators == null || separators.isEmpty()) {
            throw new IllegalArgumentException("separators must not be empty");
        }

        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
        this.separators = List.copyOf(separators);
    }

    public List<String> split(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> splits = splitRecursively(text.trim(), 0);
        return mergeSplits(splits);
    }

    private List<String> splitRecursively(String text, int separatorIndex) {
        if (codePointLength(text) <= chunkSize) {
            return List.of(text);
        }

        if (separatorIndex >= separators.size()) {
            return splitIntoCodePoints(text);
        }

        String separator = separators.get(separatorIndex);
        if (separator.isEmpty()) {
            return splitIntoCodePoints(text);
        }

        List<String> parts = splitKeepingSeparator(text, separator);
        if (parts.size() == 1) {
            return splitRecursively(text, separatorIndex + 1);
        }

        List<String> result = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }

            if (codePointLength(part) <= chunkSize) {
                result.add(part);
            } else {
                result.addAll(splitRecursively(part, separatorIndex + 1));
            }
        }

        return result;
    }

    private List<String> splitKeepingSeparator(String text, String separator) {
        List<String> result = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int index = text.indexOf(separator, start);
            if (index < 0) {
                result.add(text.substring(start));
                break;
            }

            int end = index + separator.length();
            result.add(text.substring(start, end));
            start = end;
        }

        return result;
    }

    private List<String> splitIntoCodePoints(String text) {
        List<String> result = new ArrayList<>(codePointLength(text));
        int offset = 0;
        while (offset < text.length()) {
            int codePoint = text.codePointAt(offset);
            int nextOffset = offset + Character.charCount(codePoint);
            result.add(text.substring(offset, nextOffset));
            offset = nextOffset;
        }
        return result;
    }

    private List<String> mergeSplits(List<String> splits) {
        List<String> chunks = new ArrayList<>();
        List<String> currentSplits = new ArrayList<>();
        int currentLength = 0;

        for (String split : splits) {
            if (split == null || split.isBlank()) {
                continue;
            }

            int splitLength = codePointLength(split);

            if (currentLength + splitLength > chunkSize) {
                addChunk(chunks, currentSplits);

                while (currentLength > chunkOverlap && !currentSplits.isEmpty()) {
                    String removed = currentSplits.remove(0);
                    currentLength -= codePointLength(removed);
                }

                while (currentLength + splitLength > chunkSize && !currentSplits.isEmpty()) {
                    String removed = currentSplits.remove(0);
                    currentLength -= codePointLength(removed);
                }
            }

            currentSplits.add(split);
            currentLength += splitLength;
        }

        addChunk(chunks, currentSplits);
        return chunks;
    }

    private void addChunk(List<String> chunks, List<String> splits) {
        if (splits.isEmpty()) {
            return;
        }

        String chunk = String.join("", splits).trim();
        if (!chunk.isBlank()) {
            chunks.add(chunk);
        }
    }

    private int codePointLength(String text) {
        return text.codePointCount(0, text.length());
    }
}
