# --- !Ups

create table project_competition_entries(
  id              bigserial not null primary key,
  created_at      timestamp not null,
  project_id      bigint    not null references projects on delete cascade,
  user_id         bigint    not null references users on delete cascade,
  competition_id  bigint    not null references project_competitions on delete cascade,
  unique (project_id, user_id, competition_id)
);

create table project_competition_entry_votes(
  user_id   bigint  not null references users on delete cascade,
  entry_id  bigint  not null references project_competition_entries on delete cascade,
  primary key (user_id, entry_id)
);

# --- !Downs

drop table project_competition_entry_votes;
drop table project_competition_entries;
