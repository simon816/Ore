# --- !Ups

create table organizations (
  id           BIGSERIAL          NOT NULL PRIMARY KEY,
  created_at   TIMESTAMP          NOT NULL,
  name         VARCHAR(20)        UNIQUE NOT NULL,
  password     VARCHAR(100)       NOT NULL,
  owner_id     BIGINT             NOT NULL REFERENCES users ON DELETE CASCADE,
  avatar_url   VARCHAR(255),
  tagline      VARCHAR(255),
  global_roles INT []             NOT NULL DEFAULT '{}'
);

# --- !Downs

drop table organizations;
