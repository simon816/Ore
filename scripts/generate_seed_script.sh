#!/usr/bin/env bash

time="TIMESTAMP '2016-04-06 19:24:15.394'"

for i in $(seq 0 200); do
  user="'User-$i'"
  echo "INSERT INTO users (external_id, created_at, username, email) VALUES ($i, $time, $user, 'spongie@spongepowered.org');" \
       "INSERT INTO projects (id, created_at, plugin_id, name, slug, owner_name, owner_id, authors, " \
                             "recommended_version_id, category_id, views, downloads, stars) " \
       "VALUES ($i, $time, 'pluginId.$i', 'Project $i', 'Project-$i', $user, $i, '{}', $i, 0, 0, 0, 0);" \
       "INSERT INTO channels (id, created_at, name, color_id, project_id) " \
       "VALUES ($i, $time, 'Release', 7, $i);" \
       "INSERT INTO versions (id, created_at, version_string, dependencies, downloads, project_id, channel_id) " \
       "VALUES ($i, $time, '1.0.0', '{}', 0, $i, $i);"
done
