# --- !Ups

CREATE TABLE projects (
  id            serial        NOT NULL PRIMARY KEY,
  created_at    timestamp     NOT NULL,
  plugin_id     varchar(255)  NOT NULL,
  name          varchar(255)  NOT NULL,
  description   varchar(255)  NOT NULL,
  owner_name    varchar(255)  NOT NULL,
  views         bigint        NOT NULL,
  downloads     bigint        NOT NULL,
  starred       bigint        NOT NULL
);

CREATE TABLE channels (
  id          serial        NOT NULL PRIMARY KEY,
  created_at  timestamp     NOT NULL,
  project_id  bigint        NOT NULL,
  name        varchar(255)  NOT NULL,
  color_hex   varchar(255)  NOT NULL
);

CREATE TABLE versions (
  id              serial        NOT NULL PRIMARY KEY,
  created_at      timestamp     NOT NULL,
  project_id      BIGINT        NOT NULL,
  channel_id      bigint        NOT NULL,
  version_string  varchar(255)  NOT NULL
);

CREATE TABLE devs (
  id          serial        NOT NULL PRIMARY KEY,
  created_at  timestamp     NOT NULL,
  name        varchar(255)  NOT NULL
);

CREATE TABLE teams (
  id          serial        NOT NULL PRIMARY KEY,
  created_at  timestamp     NOT NULL,
  name        varchar(255)  NOT NULL
);

# --- !Downs

DROP TABLE projects;
DROP TABLE channels;
DROP TABLE versions;
DROP TABLE devs;
DROP TABLE teams;
