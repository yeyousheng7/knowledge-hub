alter table note
    add column category_id BIGINT unsigned null after summary;

alter table note
    add key idx_note_user_category_updated (user_id, category_id, deleted, updated_at, id);