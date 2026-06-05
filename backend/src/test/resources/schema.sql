drop table if exists app_user;
drop table if exists note;
drop table if exists category;
drop table if exists tag;
drop table if exists note_tag;

create table app_user
(
    id            bigint auto_increment not null,
    username      varchar(30)           not null,
    password_hash varchar(255)          not null,
    nickname      varchar(30),
    bio           varchar(60),
    role          varchar(20)           not null default 'USER',
    status        varchar(20)           not null default 'ENABLED',
    created_at    datetime              not null,
    updated_at    datetime              not null,
    primary key (id),
    unique key uk_app_user_username (username)
);

create table note
(
    id                bigint auto_increment not null,
    user_id           bigint                not null,
    title             varchar(100)          not null,
    content_md        mediumtext            null,
    summary           varchar(300)          null,
    visibility        varchar(20)           not null default 'PRIVATE',
    created_at        datetime(3)           not null,
    updated_at        datetime(3)           not null,
    published_at      datetime(3)           null,
    moderation_status varchar(20)           not null default 'NORMAL',
    moderated_at      datetime(3)           null,
    deleted           tinyint(1) unsigned   not null default 0,
    deleted_at        datetime(3)           null,
    primary key (id),
    key idx_note_user_updated (user_id, deleted, updated_at, id),
    key idx_note_public_published (visibility, deleted, moderation_status, published_at, id),
    key idx_note_user_public_published (user_id, visibility, deleted, moderation_status, published_at, id)
);

create table category
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
    unique key uk_category_user_name_deleted_marker (user_id, name, deleted_marker),
    key idx_category_user_updated (user_id, deleted, updated_at)
);

alter table note
    add column category_id BIGINT unsigned null after summary;

alter table note
    add key idx_note_user_category_updated (user_id, category_id, deleted, updated_at, id);

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
);

create table note_tag
(
    note_id    BIGINT unsigned not null,
    tag_id     BIGINT unsigned not null,
    created_at DATETIME(3)     not null,

    primary key (note_id, tag_id),
    key idx_note_tag_tag_note (tag_id, note_id)
)
