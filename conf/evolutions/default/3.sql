# --- !Ups

CREATE TABLE starred_projects (
  user_id     bigint    NOT NULL REFERENCES users ON DELETE CASCADE,
  project_id  bigint    NOT NULL REFERENCES projects ON DELETE CASCADE,
  PRIMARY KEY (user_id, project_id)
);

# --- !Downs

DROP TABLE starred_projects;
