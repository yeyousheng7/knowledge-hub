-- knowledge_hub.app_user 定义

create table app_user (
                          id BIGINT unsigned auto_increment not null,
                          username varchar(30) not null,
                          password_hash varchar(255) not null,
                          nickname varchar(30) null,
                          bio VARCHAR(60) null,
                          `role` varchar(20) default 'USER' not null,
                          status varchar(20) default 'ENABLED' not null,
                          created_at DATETIME(3) not null,
                          updated_at DATETIME(3) not null,
                          primary key (id),
                          unique key uk_app_user_username (username)
)
engine = InnoDB
default CHARSET = utf8mb4
collate = utf8mb4_unicode_ci;
