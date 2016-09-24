# --- !Ups

alter table projects drop column stars;
alter table projects drop column views;

# --- !Downs

alter table projects add column stars int default 0;
alter table projects add column views int default 0;
