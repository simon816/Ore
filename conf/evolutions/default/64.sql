# --- !Ups

alter table projects add column needs_review boolean not null default true;

# --- !Downs

alter table projects drop column needs_review;
