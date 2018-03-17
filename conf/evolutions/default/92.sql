 # --- !Ups
CREATE TABLE user_action_log (
  id         SERIAL PRIMARY KEY,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  user_id    INT REFERENCES users,
  address    INET NOT NULL,
  action     TEXT
);

# --- !Downs
DROP TABLE user_action_log;
