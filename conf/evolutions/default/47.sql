# --- !Ups

create table user_organization_roles (
  id                bigserial   not null primary key,
  created_at        timestamp   not null,
  user_id           bigint      not null references users on delete cascade,
  role_type         int         not null,
  organization_id   bigint      not null references organizations on delete cascade,
  is_accepted       boolean     not null default false
);

# --- !Downs

drop table user_organization_roles;
