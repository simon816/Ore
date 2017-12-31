# --- !Ups

-- create the tags table
CREATE TABLE project_tags (
  id         SERIAL,
  created_at TIMESTAMP,
  project_id BIGINT,
  name       VARCHAR(255),
  data       VARCHAR(255),
  color      INT
);

-- add the tags column
ALTER TABLE projects
  ADD COLUMN tags VARCHAR(255) DEFAULT '';

-- migrate the sponge versions to the tags table
WITH sponge_projects AS (
  -- get the projects that have is_sponge_plugin set to true
    SELECT
      id AS project_id,
      created_at
    FROM projects
    WHERE is_sponge_plugin = TRUE
)
  , sponge_tag_info AS (
    SELECT
      id       AS project_id,
      project_versions.created_at,
      -- split the sponge version string and get the version number
      split_part(
      -- get the dependency that starts with "spongeapi:"
          (unnest(dependencies) ILIKE 'spongeapi:%') [1],
          ':', 2
      )        AS data,
      -- TODO Correct Sponge Color (This is Orange in ore/Colors.scala)
      13       AS color,
      'Sponge' AS name
    FROM project_versions
      INNER JOIN sponge_projects ON project_versions.id = sponge_projects.project_id
)
INSERT INTO project_tags
  SELECT *
  FROM sponge_tag_info;

-- migrate the forge versions to the tags table (duplicate of above)
WITH forge_projects AS (
    SELECT
      id AS project_id,
      created_at
    FROM projects
    WHERE is_forge_mod = TRUE
),
    forge_tag_info AS (
      SELECT
        id      AS project_id,
        project_versions.created_at,
        -- split the forge version string and get the version number
        split_part(
        -- get the dependency that starts with "Forge:"
            (unnest(dependencies) ILIKE 'Forge:%') [1],
            ':', 2
        )       AS data,
        -- TODO Correct Forge Color (This is Red in ore/Colors.scala)
        14      AS color,
        'Forge' AS name
      FROM project_versions
        INNER JOIN forge_projects ON project_versions.id = forge_projects.project_id
  )
INSERT INTO project_tags
  SELECT *
  FROM forge_tag_info;

-- Remove the boolean fields
ALTER TABLE projects
  ALTER COLUMN is_sponge_plugin DROP DEFAULT;
ALTER TABLE projects
  ALTER COLUMN is_forge_mod DROP DEFAULT;

# --- !Downs

-- TODO
