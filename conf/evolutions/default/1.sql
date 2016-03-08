# --- !Ups

CREATE TABLE projects (
  id            bigserial     NOT NULL PRIMARY KEY,
  created_at    timestamp     NOT NULL DEFAULT now(),
  plugin_id     varchar(255)  NOT NULL,
  name          varchar(255)  NOT NULL,
  description   varchar(255)  NOT NULL,
  owner_name    varchar(255)  NOT NULL,
  views         bigint        NOT NULL DEFAULT 0,
  downloads     bigint        NOT NULL DEFAULT 0,
  starred       bigint        NOT NULL DEFAULT 0
);

CREATE TABLE channels (
  id          bigserial     NOT NULL PRIMARY KEY,
  created_at  timestamp     NOT NULL DEFAULT now(),
  project_id  bigint        NOT NULL,
  name        varchar(255)  NOT NULL,
  color_hex   varchar(255)  NOT NULL DEFAULT '#2ECC40'
);

CREATE TABLE versions (
  id              bigserial     NOT NULL PRIMARY KEY,
  created_at      timestamp     NOT NULL DEFAULT now(),
  channel_id      bigint        NOT NULL,
  version_string  varchar(255)  NOT NULL
);

CREATE TABLE devs (
  id          bigserial     NOT NULL PRIMARY KEY,
  created_at  timestamp     NOT NULL DEFAULT now(),
  name        varchar(255)  NOT NULL
);

CREATE TABLE teams (
  id          bigserial     NOT NULL PRIMARY KEY,
  created_at  timestamp     NOT NULL DEFAULT now(),
  name        varchar(255)  NOT NULL
);

# --- !Downs

DROP TABLE projects;
DROP TABLE channels;
DROP TABLE versions;
DROP TABLE devs;
DROP TABLE teams;
