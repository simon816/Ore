# --- !Ups

CREATE TABLE project_views (
  id          serial        NOT NULL PRIMARY KEY,
  cookie      varchar(50)   ,
  user_id     bigint        REFERENCES users ON DELETE CASCADE,
  project_id  bigint        NOT NULL REFERENCES projects ON DELETE CASCADE
);

CREATE TABLE version_downloads (
  id          serial        NOT NULL PRIMARY KEY,
  cookie      varchar(50)   ,
  user_id     bigint        REFERENCES users ON DELETE CASCADE,
  version_id  bigint        NOT NULL REFERENCES versions ON DELETE CASCADE
);

# --- !Downs

DROP TABLE project_views;
DROP TABLE version_downloads;
