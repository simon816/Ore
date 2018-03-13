 # --- !Ups
CREATE TABLE user_action_log (
  id     SERIAL PRIMARY KEY,
  time   TIMESTAMP NOT NULL DEFAULT NOW(),
  userId INT REFERENCES users,
  address INET NOT NULL,
  action TEXT
);

# --- !Downs
DROP TABLE user_action_log;
