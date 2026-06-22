package com.yousheng.knowledgehub.ai.index;

import com.yousheng.knowledgehub.ai.config.AiProperties;
import com.yousheng.knowledgehub.ai.index.model.NoteChunk;
import com.yousheng.knowledgehub.note.entity.Note;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NoteChunkBuilderTest {

    @Test
    void build_validNote_generatesCleanedChunk() {
        Note note = note();
        note.setContentMd("# Intro\n\nHello [Spring](https://spring.io).");
        NoteChunkBuilder builder = noteChunkBuilder();

        List<NoteChunk> chunks = builder.build(note);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).text()).isEqualTo("Index Note\nShort summary\nIntro\n\nHello Spring.");
    }

    @Test
    void build_validNote_populatesMetadata() {
        Note note = note();
        NoteChunkBuilder builder = noteChunkBuilder();

        NoteChunk chunk = builder.build(note).get(0);

        assertThat(chunk.chunkId()).isEqualTo("note:100:chunk:0");
        assertThat(chunk.userId()).isEqualTo(10L);
        assertThat(chunk.noteId()).isEqualTo(100L);
        assertThat(chunk.chunkIndex()).isZero();
        assertThat(chunk.title()).isEqualTo("Index Note");
        assertThat(chunk.visibility()).isEqualTo("PUBLIC");
        assertThat(chunk.updatedAt()).isEqualTo(LocalDateTime.of(2026, 6, 22, 10, 0));
        assertThat(chunk.contentHash()).isEqualTo(expectedHash("Index Note\nShort summary\nHello **world**."));
    }

    @Test
    void build_markdownSyntaxCleaned_hashesCleanedText() {
        Note note = note();
        note.setContentMd("# Intro\n\nHello [Spring](https://spring.io).");
        NoteChunkBuilder builder = noteChunkBuilder();

        String contentHash = builder.build(note).get(0).contentHash();

        assertThat(contentHash).isEqualTo(expectedHash("Index Note\nShort summary\nIntro\n\nHello Spring."));
        assertThat(contentHash).isNotEqualTo(expectedHash(
                "Index Note\nShort summary\n# Intro\n\nHello [Spring](https://spring.io)."
        ));
    }

    @Test
    void build_sameContentWithDifferentUpdatedAt_keepsSameContentHash() {
        Note first = note();
        Note second = note();
        second.setUpdatedAt(LocalDateTime.of(2026, 6, 23, 10, 0));
        NoteChunkBuilder builder = noteChunkBuilder();

        String firstHash = builder.build(first).get(0).contentHash();
        String secondHash = builder.build(second).get(0).contentHash();

        assertThat(secondHash).isEqualTo(firstHash);
    }

    @Test
    void build_changedSummary_changesContentHash() {
        Note first = note();
        Note second = note();
        second.setSummary("Changed summary");
        NoteChunkBuilder builder = noteChunkBuilder();

        String firstHash = builder.build(first).get(0).contentHash();
        String secondHash = builder.build(second).get(0).contentHash();

        assertThat(secondHash).isNotEqualTo(firstHash);
    }

    @Test
    void build_longContent_generatesMultipleChunks() {
        Note note = note();
        note.setContentMd("abcdefghijklmnopqrstuvwxyz");
        NoteChunkBuilder builder = noteChunkBuilder(10, 0);

        List<NoteChunk> chunks = builder.build(note);

        assertThat(chunks).hasSize(7);
        assertThat(chunks).extracting(NoteChunk::chunkId)
                .containsExactly(
                        "note:100:chunk:0",
                        "note:100:chunk:1",
                        "note:100:chunk:2",
                        "note:100:chunk:3",
                        "note:100:chunk:4",
                        "note:100:chunk:5",
                        "note:100:chunk:6"
                );
        assertThat(chunks).extracting(NoteChunk::chunkIndex)
                .containsExactly(0, 1, 2, 3, 4, 5, 6);
        assertThat(chunks).extracting(NoteChunk::text)
                .containsExactly("Index", "Note", "Short", "summary\nab", "cdefghijkl", "mnopqrstuv", "wxyz");
    }

    @Test
    void build_blankContentWithTitleAndSummary_generatesChunkFromTitleAndSummary() {
        Note note = note();
        note.setContentMd("   \n  ");
        NoteChunkBuilder builder = noteChunkBuilder();

        List<NoteChunk> chunks = builder.build(note);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).text()).isEqualTo("Index Note\nShort summary");
    }

    @Test
    void build_nullContentMdWithTitle_generatesChunkFromTitleAndSummary() {
        Note note = note();
        note.setContentMd(null);
        NoteChunkBuilder builder = noteChunkBuilder();

        List<NoteChunk> chunks = builder.build(note);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).text()).isEqualTo("Index Note\nShort summary");
    }

    @Test
    void build_cleanedBlankContent_indexesTitleAndSummary() {
        Note note = note();
        note.setContentMd("```java\n```");
        NoteChunkBuilder builder = noteChunkBuilder();

        List<NoteChunk> chunks = builder.build(note);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).text()).isEqualTo("Index Note\nShort summary");
    }

    @Test
    void build_invalidInput_throwsException() {
        NoteChunkBuilder builder = noteChunkBuilder();

        assertThatThrownBy(() -> builder.build(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("note must not be null");

        Note missingId = note();
        missingId.setId(null);
        assertThatThrownBy(() -> builder.build(missingId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("note.id");

        Note missingUserId = note();
        missingUserId.setUserId(null);
        assertThatThrownBy(() -> builder.build(missingUserId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("note.userId");

        Note blankTitle = note();
        blankTitle.setTitle(" ");
        assertThatThrownBy(() -> builder.build(blankTitle))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("note.title");
    }

    private static Note note() {
        Note note = new Note();
        note.setId(100L);
        note.setUserId(10L);
        note.setTitle("Index Note");
        note.setContentMd("Hello **world**.");
        note.setSummary("Short summary");
        note.setVisibility("PUBLIC");
        note.setUpdatedAt(LocalDateTime.of(2026, 6, 22, 10, 0));
        return note;
    }

    private static NoteChunkBuilder noteChunkBuilder() {
        return noteChunkBuilder(1024, 50);
    }

    private static NoteChunkBuilder noteChunkBuilder(int chunkSize, int chunkOverlap) {
        return new NoteChunkBuilder(aiProperties(chunkSize, chunkOverlap));
    }

    private static AiProperties aiProperties(int chunkSize, int chunkOverlap) {
        AiProperties properties = new AiProperties();
        AiProperties.Index index = new AiProperties.Index();
        index.setChunkSize(chunkSize);
        index.setChunkOverlap(chunkOverlap);
        properties.setIndex(index);
        return properties;
    }

    private static String expectedHash(String value) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(messageDigest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
