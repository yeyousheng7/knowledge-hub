-- knowledge_hub.note 定义

create table note
(
    id                BIGINT unsigned auto_increment    not null,
    user_id           BIGINT unsigned                   not null,
    title             varchar(100)                      not null,
    content_md        MEDIUMTEXT                        null,
    summary           varchar(300)                      null,
    visibility        varchar(20) default 'PRIVATE'     not null,
    created_at        DATETIME(3)                       not null,
    updated_at        DATETIME(3)                       not null,
    published_at      DATETIME(3)                       null,
    moderation_status varchar(20) default 'NORMAL'      not null,
    moderated_at      DATETIME(3)                       null,
    deleted           tinyint(1) unsigned default 0     not null,
    deleted_at        DATETIME(3)                       null,

    primary key (id),

    key idx_note_user_updated (user_id, deleted, updated_at, id),
    key idx_note_public_published (visibility, deleted, moderation_status, published_at, id),
    key idx_note_user_public_published (user_id, visibility, deleted, moderation_status, published_at, id)
)
    engine = InnoDB
    default charset = utf8mb4
    collate = utf8mb4_unicode_ci;