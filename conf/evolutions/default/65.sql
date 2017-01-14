# --- !Ups

create table project_version_reviews(
  id          bigserial not null primary key,
  created_at  timestamp not null,
  version_id  bigint    not null references project_versions on delete cascade,
  project_id  bigint    not null references projects on delete cascade,
  assignee_id bigint    not null references users on delete restrict,
  status      int       not null
);

# --- !Downs

drop table project_version_reviews;
