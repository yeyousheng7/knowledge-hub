-- knowledge_hub.tag 和 knowledge_hub.note_tag 定义

create table tag
(
    id             BIGINT unsigned auto_increment not null,
    user_id        BIGINT unsigned                not null,
    name           varchar(30)                    not null,
    created_at     DATETIME(3)                    not null,
    updated_at     DATETIME(3)                    not null,
    deleted        tinyint(1) unsigned default 0  not null,
    deleted_marker BIGINT unsigned     default 0  not null,
    deleted_at     DATETIME(3)                    null,

    primary key (id),
    unique key uk_tag_user_name_deleted_marker (user_id, name, deleted_marker),
    key idx_tag_user_deleted_updated (user_id, deleted, updated_at)
)
    engine = InnoDB
    default CHARSET = utf8mb4
    collate = utf8mb4_unicode_ci;

create table note_tag
(
    note_id    BIGINT unsigned not null,
    tag_id     BIGINT unsigned not null,
    created_at DATETIME(3)     not null,

    primary key (note_id, tag_id),
    key idx_note_tag_tag_note (tag_id, note_id)
)
    engine = InnoDB
    default charset = utf8mb4
    collate = utf8mb4_unicode_ci;
