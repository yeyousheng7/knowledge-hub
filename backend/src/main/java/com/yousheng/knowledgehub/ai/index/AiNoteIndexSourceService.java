package com.yousheng.knowledgehub.ai.index;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yousheng.knowledgehub.ai.index.model.NoteChunk;
import com.yousheng.knowledgehub.common.constant.SoftDeleteConstants;
import com.yousheng.knowledgehub.note.entity.Note;
import com.yousheng.knowledgehub.note.mapper.NoteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class AiNoteIndexSourceService {
    private final NoteMapper noteMapper;
    private final NoteChunkBuilder noteChunkBuilder;

    public List<NoteChunk> loadChunks(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }

        LambdaQueryWrapper<Note> query = Wrappers.lambdaQuery(Note.class)
                .eq(Note::getUserId, userId)
                .eq(Note::getDeleted, SoftDeleteConstants.NOT_DELETED);

        List<Note> notes = noteMapper.selectList(query);

        return notes.stream()
                .flatMap(note -> noteChunkBuilder.build(note).stream())
                .toList();
    }
}
