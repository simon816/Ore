# --- !Ups

CREATE TABLE projects (
  id                      serial          NOT NULL PRIMARY KEY,
  created_at              timestamp       NOT NULL,
  plugin_id               varchar(255)    NOT NULL UNIQUE,
  name                    varchar(255)    NOT NULL,
  owner_name              varchar(255)    NOT NULL,
  authors                 varchar(255)[]  NOT NULL,
  homepage                text            ,
  recommended_version_id  bigint          ,
  views                   bigint          NOT NULL CHECK (views >= 0),
  downloads               bigint          NOT NULL CHECK (downloads >= 0),
  starred                 bigint          NOT NULL CHECK (starred >= 0)
);

CREATE TABLE channels (
  id          serial        NOT NULL PRIMARY KEY,
  created_at  timestamp     NOT NULL,
  name        varchar(255)  NOT NULL,
  color_hex   varchar(255)  NOT NULL,
  project_id  bigint        NOT NULL REFERENCES projects ON DELETE CASCADE
);

CREATE TABLE versions (
  id              serial          NOT NULL PRIMARY KEY,
  created_at      timestamp       NOT NULL,
  version_string  varchar(255)    NOT NULL,
  dependencies    varchar(255)[]  NOT NULL,
  description     varchar(255)    ,
  assets          varchar(510)    ,
  downloads       bigint          NOT NULL CHECK (downloads >= 0),
  project_id      bigint          NOT NULL REFERENCES projects ON DELETE CASCADE,
  channel_id      bigint          NOT NULL REFERENCES channels ON DELETE CASCADE
);

CREATE TABLE users (
  external_id  bigint        NOT NULL PRIMARY KEY,
  created_at   timestamp     NOT NULL,
  name         varchar(255)  NOT NULL,
  username     varchar(255)  NOT NULL,
  email        varchar(255)  NOT NULL
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
