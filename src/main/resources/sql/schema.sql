
CREATE TABLE tags (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(20)
);

CREATE TABLE game_object_tags (
    id BIGSERIAL PRIMARY KEY,
    game_object_id BIGINT REFERENCES game_objects(id),
    tag_id BIGINT REFERENCES tags(id)
);

ALTER TABLE game_object_tags ADD CONSTRAINT uq_game_object_id_tag_id UNIQUE (game_object_id, tag_id);
