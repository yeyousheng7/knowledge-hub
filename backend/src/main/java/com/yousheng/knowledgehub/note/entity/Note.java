package com.yousheng.knowledgehub.note.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@TableName("note")
public class Note {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String contentMd;

    private String summary;

    private String visibility;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime publishedAt;

    private String moderationStatus;

    private LocalDateTime moderatedAt;

    private Integer deleted;

    private LocalDateTime deletedAt;

}
