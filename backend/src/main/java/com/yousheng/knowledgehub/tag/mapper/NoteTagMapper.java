package com.yousheng.knowledgehub.tag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yousheng.knowledgehub.note.dto.NoteTagResponse;
import com.yousheng.knowledgehub.tag.dto.NoteTagQueryRow;
import com.yousheng.knowledgehub.tag.entity.NoteTag;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface NoteTagMapper extends BaseMapper<NoteTag> {

    @Select("""
            SELECT t.id, t.name
            FROM note_tag nt
            JOIN tag t ON t.id = nt.tag_id
            WHERE nt.note_id = #{noteId}
              AND t.deleted = 0
            ORDER BY t.id ASC
            """)
    List<NoteTagResponse> selectTagResponseByNoteId(@Param("noteId") Long noteId);

    @Select("""
            <script>
            SELECT nt.note_id AS noteId,
                   t.id AS tagId,
                   t.name AS tagName
            FROM note_tag nt
            JOIN tag t ON t.id = nt.tag_id
            WHERE nt.note_id IN
            <foreach collection="noteIds"
                     item="noteId"
                     open="("
                     separator=","
                     close=")">
                #{noteId}
            </foreach>
              AND t.deleted = 0
            ORDER BY nt.note_id ASC, t.id ASC
            </script>
            """)
    List<NoteTagQueryRow> selectTagRowsByNoteIds(
            @Param("noteIds") List<Long> noteIds
    );
}
