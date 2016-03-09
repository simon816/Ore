# --- !Ups
ALTER TABLE versions ADD project_id bigint NOT NULL;

# --- !Downs
ALTER TABLE versions DROP project_id;
