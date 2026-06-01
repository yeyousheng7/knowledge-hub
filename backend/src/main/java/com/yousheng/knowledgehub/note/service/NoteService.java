package com.yousheng.knowledgehub.note.service;

import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.note.dto.NoteCreateRequest;
import com.yousheng.knowledgehub.note.dto.NoteCreateResponse;
import com.yousheng.knowledgehub.note.entity.Note;
import com.yousheng.knowledgehub.note.enums.NoteModerationStatus;
import com.yousheng.knowledgehub.note.enums.NoteVisibility;
import com.yousheng.knowledgehub.note.mapper.NoteMapper;
import com.yousheng.knowledgehub.security.CurrentUser;
import com.yousheng.knowledgehub.user.entity.AppUser;
import com.yousheng.knowledgehub.user.enums.UserStatus;
import com.yousheng.knowledgehub.user.mapper.AppUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class NoteService {
    private final AppUserMapper appUserMapper;
    private final NoteMapper noteMapper;

    @Transactional
    public NoteCreateResponse createNote(NoteCreateRequest request) {
        Long userId = CurrentUser.getUserId();
        AppUser user = appUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        if (!UserStatus.ENABLED.name().equals(user.getStatus())) {
            throw new BizException(ErrorCode.USER_DISABLED);
        }

        LocalDateTime now = LocalDateTime.now();

        Note note = new Note();
        note.setUserId(userId);
        note.setTitle(request.title().trim());
        note.setContentMd(request.contentMd());
        note.setSummary(request.summary());
        note.setVisibility(NoteVisibility.PRIVATE.name());
        note.setModerationStatus(NoteModerationStatus.NORMAL.name());
        note.setCreatedAt(now);
        note.setUpdatedAt(now);
        note.setDeleted(0);

        noteMapper.insert(note);

        return new NoteCreateResponse(
                note.getId(),
                note.getTitle(),
                note.getVisibility(),
                note.getModerationStatus(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }
}
