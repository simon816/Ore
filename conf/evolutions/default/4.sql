# --- !Ups

ALTER TABLE versions ALTER description SET DATA TYPE text;

# --- !Downs

ALTER TABLE versions ALTER description SET DATA TYPE varchar(255);
