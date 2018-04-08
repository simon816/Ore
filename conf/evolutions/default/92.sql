 # --- !Ups
 CREATE TABLE user_action_log (
   id         SERIAL PRIMARY KEY,
   created_at TIMESTAMP NOT NULL DEFAULT NOW(),
   user_id    INT REFERENCES users,
   address    INET NOT NULL,
   context    VARCHAR(40) NOT NULL,
   context_id INT,
   action     VARCHAR(40) NOT NULL,
   new_state  TEXT,
   old_state  TEXT
 );

# --- !Downs
DROP TABLE user_action_log;
