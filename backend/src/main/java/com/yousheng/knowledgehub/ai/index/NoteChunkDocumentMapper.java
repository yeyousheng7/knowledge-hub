package com.yousheng.knowledgehub.ai.index;

import com.yousheng.knowledgehub.ai.index.model.NoteChunk;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class NoteChunkDocumentMapper {

    public Document toDocument(NoteChunk noteChunk) {
        if (noteChunk == null) {
            throw new IllegalArgumentException("noteChunk must not be null");
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userId", noteChunk.userId());
        metadata.put("noteId", noteChunk.noteId());
        metadata.put("title", noteChunk.title());
        metadata.put("chunkIndex", noteChunk.chunkIndex());
        metadata.put("visibility", noteChunk.visibility());
        metadata.put("updatedAt", noteChunk.updatedAt());
        metadata.put("contentHash", noteChunk.contentHash());

        return Document.builder()
                .id(noteChunk.chunkId())
                .text(noteChunk.text())
                .metadata(metadata)
                .build();
    }

    public List<Document> toDocuments(List<NoteChunk> noteChunks) {
        if (noteChunks == null) {
            throw new IllegalArgumentException("noteChunks must not be null");
        }

        return noteChunks.stream()
                .map(this::toDocument)
                .toList();
    }
}
