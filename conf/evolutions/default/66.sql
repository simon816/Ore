# --- !Ups

alter table project_version_reviews drop column assignee_id;
alter table project_version_reviews add column assignee_id bigint not null default -1;

# --- !Downs
