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

create table organization_members (
  user_id           bigint not null references users on delete cascade,
  organization_id   bigint not null references organizations on delete cascade,
  primary key (user_id, organization_id)
);

alter table projects add column organization_id bigint REFERENCES organizations;

# --- !Downs

drop table organizations;
drop table organization_members;
