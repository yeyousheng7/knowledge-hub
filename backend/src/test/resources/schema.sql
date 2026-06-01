drop table if exists app_user;
drop table if exists note;

create table app_user (
                          id bigint auto_increment not null,
                          username varchar(30) not null,
                          password_hash varchar(255) not null,
                          nickname varchar(30),
                          bio varchar(60),
                          role varchar(20) not null default 'USER',
                          status varchar(20) not null default 'ENABLED',
                          created_at datetime not null,
                          updated_at datetime not null,
                          primary key (id),
                          unique key uk_app_user_username (username)
);

create table note (
                     id bigint auto_increment not null,
                     user_id bigint not null,
                     title varchar(100) not null,
                     content_md mediumtext null,
                     summary varchar(300) null,
                     visibility varchar(20) not null default 'PRIVATE',
                     created_at datetime(3) not null,
                     updated_at datetime(3) not null,
                     published_at datetime(3) null,
                     moderation_status varchar(20) not null default 'NORMAL',
                     moderated_at datetime(3) null,
                     deleted tinyint(1) unsigned not null default 0,
                     deleted_at datetime(3) null,
                     primary key (id),
                     key idx_note_user_updated (user_id, deleted, updated_at, id),
                     key idx_note_public_published (visibility, deleted, moderation_status, published_at, id),
                     key idx_note_user_public_published (user_id, visibility, deleted, moderation_status, published_at, id)
);