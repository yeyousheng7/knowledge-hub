package com.yousheng.knowledgehub.ai.index;

import com.yousheng.knowledgehub.ai.index.model.NoteChunk;
import com.yousheng.knowledgehub.note.entity.Note;
import com.yousheng.knowledgehub.note.mapper.NoteMapper;
import com.yousheng.knowledgehub.user.entity.AppUser;
import com.yousheng.knowledgehub.user.enums.UserRole;
import com.yousheng.knowledgehub.user.enums.UserStatus;
import com.yousheng.knowledgehub.user.mapper.AppUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
class AiNoteIndexSourceServiceTest {

    @Autowired
    private AiNoteIndexSourceService service;

    @Autowired
    private AppUserMapper appUserMapper;

    @Autowired
    private NoteMapper noteMapper;

    @MockBean
    private NoteChunkBuilder noteChunkBuilder;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        for (String table : List.of("note_tag", "note", "tag", "category", "app_user")) {
            jdbcTemplate.execute("DELETE FROM " + table);
        }
    }

    @Test
    void loadChunks_onlyReturnsTargetUserNonDeletedNotes() {
        AppUser user1 = createUser("user1");
        AppUser user2 = createUser("user2");

        Note target = createNote(user1.getId(), "Target note", 0);
        createNote(user2.getId(), "Other user note", 0);
        createNote(user1.getId(), "Deleted note", 1);

        when(noteChunkBuilder.build(any())).thenAnswer(inv -> {
            Note note = inv.getArgument(0);
            return List.of(chunkOf(note));
        });

        List<NoteChunk> chunks = service.loadChunks(user1.getId());

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).noteId()).isEqualTo(target.getId());
    }

    @Test
    void loadChunks_nullUserId_throwsException() {
        assertThatThrownBy(() -> service.loadChunks(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId must not be null");
    }

    @Test
    void loadChunks_noNotes_returnsEmptyList() {
        AppUser user = createUser("user");

        List<NoteChunk> chunks = service.loadChunks(user.getId());

        assertThat(chunks).isEmpty();
    }

    @Test
    void loadChunks_includesAllVisibilities() {
        AppUser user = createUser("user");
        Note privateNote = createNote(user.getId(), "private", 0, "PRIVATE", "NORMAL");
        Note publicNote = createNote(user.getId(), "public", 0, "PUBLIC", "NORMAL");
        Note takenDown = createNote(user.getId(), "taken_down", 0, "PUBLIC", "TAKEN_DOWN");

        when(noteChunkBuilder.build(any())).thenAnswer(inv -> {
            Note note = inv.getArgument(0);
            return List.of(chunkOf(note));
        });

        List<NoteChunk> chunks = service.loadChunks(user.getId());

        assertThat(chunks).hasSize(3);
        assertThat(chunks).extracting(NoteChunk::noteId)
                .containsExactlyInAnyOrder(privateNote.getId(), publicNote.getId(), takenDown.getId());
    }

    private AppUser createUser(String username) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash("hash");
        user.setNickname(username);
        user.setRole(UserRole.USER.name());
        user.setStatus(UserStatus.ENABLED.name());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        appUserMapper.insert(user);
        return user;
    }

    private Note createNote(Long userId, String title, int deleted) {
        return createNote(userId, title, deleted, "PRIVATE", "NORMAL");
    }

    private Note createNote(Long userId, String title, int deleted, String visibility, String moderationStatus) {
        Note note = new Note();
        note.setUserId(userId);
        note.setTitle(title);
        note.setContentMd("content");
        note.setSummary("summary");
        note.setVisibility(visibility);
        note.setModerationStatus(moderationStatus);
        note.setDeleted(deleted);
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());
        noteMapper.insert(note);
        return note;
    }

    private static NoteChunk chunkOf(Note note) {
        return new NoteChunk(
                "note:" + note.getId() + ":chunk:0",
                note.getUserId(),
                note.getId(),
                0,
                note.getTitle(),
                "text",
                note.getVisibility(),
                note.getUpdatedAt(),
                "hash"
        );
    }
}
