package com.yousheng.knowledgehub.ai.index;

import com.yousheng.knowledgehub.ai.index.model.NoteChunk;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NoteChunkDocumentMapperTest {

    private final NoteChunkDocumentMapper mapper = new NoteChunkDocumentMapper();

    @Test
    void toDocument_mapsChunkToDocument() {
        NoteChunk chunk = chunk();
        Document doc = mapper.toDocument(chunk);

        assertThat(doc.getId()).isEqualTo("note:1:chunk:0");
        assertThat(doc.getText()).isEqualTo("Hello Spring Boot.");

        Map<String, Object> metadata = doc.getMetadata();
        assertThat(metadata).containsEntry("userId", 10L);
        assertThat(metadata).containsEntry("noteId", 1L);
        assertThat(metadata).containsEntry("title", "Spring Boot Guide");
        assertThat(metadata).containsEntry("chunkIndex", 0);
        assertThat(metadata).containsEntry("visibility", "PUBLIC");
        assertThat(metadata).containsEntry("updatedAt", "2026-06-22T10:00");
        assertThat(metadata).containsEntry("contentHash", "abc123");
    }

    @Test
    void toDocument_nullChunk_throwsException() {
        assertThatThrownBy(() -> mapper.toDocument(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("noteChunk must not be null");
    }

    @Test
    void toDocuments_mapsListOfChunks() {
        NoteChunk chunk1 = chunk();
        NoteChunk chunk2 = chunk2();

        List<Document> docs = mapper.toDocuments(List.of(chunk1, chunk2));

        assertThat(docs).hasSize(2);
        assertThat(docs.get(0).getId()).isEqualTo("note:1:chunk:0");
        assertThat(docs.get(1).getId()).isEqualTo("note:1:chunk:1");
    }

    @Test
    void toDocuments_emptyList_returnsEmptyList() {
        List<Document> docs = mapper.toDocuments(List.of());

        assertThat(docs).isEmpty();
    }

    @Test
    void toDocuments_nullList_throwsException() {
        assertThatThrownBy(() -> mapper.toDocuments(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("noteChunks must not be null");
    }

    @Test
    void toDocument_mapsEachChunkIndex() {
        NoteChunk chunk0 = chunk();
        NoteChunk chunk1 = chunk2();
        NoteChunk chunk2 = new NoteChunk(
                "note:1:chunk:2", 10L, 1L, 2,
                "Spring Boot Guide", "Conclusion.", "PUBLIC",
                LocalDateTime.of(2026, 6, 22, 10, 0), "abc123"
        );

        List<Document> docs = mapper.toDocuments(List.of(chunk0, chunk1, chunk2));

        assertThat(docs).extracting(doc -> doc.getMetadata().get("chunkIndex"))
                .containsExactly(0, 1, 2);
    }

    private static NoteChunk chunk() {
        return new NoteChunk(
                "note:1:chunk:0",
                10L,
                1L,
                0,
                "Spring Boot Guide",
                "Hello Spring Boot.",
                "PUBLIC",
                LocalDateTime.of(2026, 6, 22, 10, 0),
                "abc123"
        );
    }

    private static NoteChunk chunk2() {
        return new NoteChunk(
                "note:1:chunk:1",
                10L,
                1L,
                1,
                "Spring Boot Guide",
                "Auto-configuration simplifies setup.",
                "PUBLIC",
                LocalDateTime.of(2026, 6, 22, 10, 0),
                "abc123"
        );
    }
}
