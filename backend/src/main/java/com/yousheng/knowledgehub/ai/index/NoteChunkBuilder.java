package com.yousheng.knowledgehub.ai.index;

import com.yousheng.knowledgehub.ai.config.AiProperties;
import com.yousheng.knowledgehub.ai.index.model.NoteChunk;
import com.yousheng.knowledgehub.ai.index.util.MarkdownTextCleaner;
import com.yousheng.knowledgehub.ai.index.util.RecursiveTextSplitter;
import com.yousheng.knowledgehub.note.entity.Note;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.IntStream;

@RequiredArgsConstructor
@Component
public class NoteChunkBuilder {
    private final AiProperties aiProperties;
    private static final List<String> DEFAULT_SEPARATORS = List.of("\n\n", "\n", "。", ".", "，", ",", " ", "");

    private RecursiveTextSplitter textSplitter;

    public List<NoteChunk> build(Note note) {
        validateNote(note);

        String text = buildIndexSourceText(note);
        String cleanedText = MarkdownTextCleaner.clean(text);
        if (cleanedText.isBlank()) {
            return List.of();
        }

        prepareTextSplitter();

        String contentHash = hash(cleanedText);
        List<String> chunks = textSplitter.split(cleanedText);
        return buildChunks(note, chunks, contentHash);
    }

    private void validateNote(Note note) {
        if (note == null) {
            throw new IllegalArgumentException("note must not be null");
        }
        if (note.getId() == null) {
            throw new IllegalArgumentException("note.id must not be null");
        }
        if (note.getUserId() == null) {
            throw new IllegalArgumentException("note.userId must not be null");
        }
        if (note.getTitle() == null || note.getTitle().isBlank()) {
            throw new IllegalArgumentException("note.title must not be blank");
        }
    }

    private List<NoteChunk> buildChunks(Note note, List<String> chunks, String contentHash) {
        return IntStream.range(0, chunks.size())
                .mapToObj(index -> new NoteChunk(
                        chunkId(note.getId(), index),
                        note.getUserId(),
                        note.getId(),
                        index,
                        note.getTitle(),
                        chunks.get(index),
                        note.getVisibility(),
                        note.getUpdatedAt(),
                        contentHash
                ))
                .toList();
    }

    private String chunkId(Long noteId, int chunkIndex) {
        return "note:%d:chunk:%d".formatted(noteId, chunkIndex);
    }

    private String hash(String value) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

    private String buildIndexSourceText(Note note) {
        StringBuilder sb = new StringBuilder();
        sb.append(note.getTitle());
        if (note.getSummary() != null && !note.getSummary().isBlank()) {
            sb.append("\n").append(note.getSummary());
        }
        if (note.getContentMd() != null && !note.getContentMd().isBlank()) {
            sb.append("\n").append(note.getContentMd());
        }
        return sb.toString();
    }

    private void prepareTextSplitter() {
        if (textSplitter != null) {
            return;
        }

        textSplitter = new RecursiveTextSplitter(
                aiProperties.getIndex().getChunkSize(),
                aiProperties.getIndex().getChunkOverlap(),
                DEFAULT_SEPARATORS
        );
    }
}
