# --- !Ups
CREATE TABLE logged_actions (
   id         SERIAL PRIMARY KEY,
   created_at TIMESTAMP NOT NULL DEFAULT NOW(),
   user_id    INT REFERENCES users,
   address    INET NOT NULL,
   action     INT,
   action_context INT,
   action_context_id INT,
   new_state  TEXT,
   old_state  TEXT
);

CREATE INDEX ON logged_actions (action_context, action_context_id)

# --- !Downs
DROP TABLE logged_actions;
