# --- !Ups

create table organizations_pending(
  id            bigserial     not null primary key,
  created_at    timestamp     not null,
  user_id       bigint        not null references users on delete cascade,
  name          varchar(255)  not null,
  sponge_id     bigint        not null default -1,
  email         varchar(255),
  discourse_id  bigint        not null default -1
);

# --- !Downs

drop table organizations_pending;
