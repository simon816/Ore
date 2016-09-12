# --- !Ups

create table project_watchers (
  project_id  bigint not null,
  user_id     bigint not null,
  unique (project_id, user_id)
);

alter table projects add column organization_id bigint REFERENCES organizations;

# --- !Downs

drop table project_watchers;
