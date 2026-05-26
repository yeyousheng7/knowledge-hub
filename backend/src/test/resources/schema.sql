drop table if exists app_user;

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