# --- !Ups

-- create the tags table
CREATE TABLE project_tags (
  id         SERIAL,
  created_at TIMESTAMP,
  project_id BIGINT,
  version_id BIGINT,
  name       VARCHAR(255),
  data       VARCHAR(255),
  color      INT
);

-- add the tags column
ALTER TABLE project_versions
  ADD COLUMN tags INT [] DEFAULT NULL;

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
      project_versions.project_id,
      project_versions.id as version_id,
      project_versions.created_at,
      -- split the sponge version string and get the version number
      split_part(
      -- get the dependency that starts with "spongeapi:"
          (ARRAY(SELECT *
                 FROM unnest(dependencies) AS s(dependency_string)
                 WHERE dependency_string ILIKE 'spongeapi:%')) [1],
          ':', 2
      )        AS data,
      -- TODO Correct Sponge Color (This is Orange in ore/Colors.scala)
      13       AS color,
      'Sponge' AS name
    FROM project_versions
      INNER JOIN sponge_projects ON project_versions.project_id = sponge_projects.project_id
)
INSERT INTO project_tags (created_at, project_id, version_id, name, data, color)
  SELECT
    created_at,
    project_id,
    version_id,
    name,
    data,
    color
  FROM sponge_tag_info;

-- add the tags into the projects table
WITH sponge_projects AS (
  -- get the projects that have is_sponge_plugin set to true
    SELECT
      id AS project_id,
      created_at
    FROM projects
    WHERE is_sponge_plugin = TRUE
)
  , sponge_tags_appended AS (
    SELECT
      project_tags.version_id,
      array_agg(project_tags.id) AS tags
    FROM project_tags
      JOIN sponge_projects ON project_tags.project_id = sponge_projects.project_id
    GROUP BY project_tags.version_id
)
UPDATE project_versions
SET tags = sponge_tags_appended.tags
FROM sponge_tags_appended
WHERE project_versions.id = sponge_tags_appended.version_id;

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
        project_versions.project_id,
        project_versions.id as version_id,
        project_versions.created_at,
        -- split the forge version string and get the version number
        split_part(
        -- get the dependency that starts with "Forge:"
            (ARRAY(SELECT *
                   FROM unnest(dependencies) AS s(dependency_string)
                   WHERE dependency_string ILIKE 'Forge:%')) [1],
            ':', 2
        )       AS data,
        -- TODO Correct Forge Color (This is Red in ore/Colors.scala)
        14      AS color,
        'Forge' AS name
      FROM project_versions
        INNER JOIN forge_projects ON project_versions.project_id = forge_projects.project_id
  )
INSERT INTO project_tags (created_at, project_id, version_id, name, data, color)
  SELECT
    created_at,
    project_id,
    version_id,
    name,
    data,
    color
  FROM forge_tag_info;

-- add the tags into the projects table
WITH forge_projects AS (
  -- get the projects that have is_sponge_plugin set to true
    SELECT
      id AS project_id,
      created_at
    FROM projects
    WHERE is_forge_mod = TRUE
)
  , forge_tags_appended AS (
    SELECT
      project_tags.version_id,
      array_agg(project_tags.id) AS tags
    FROM project_tags
      JOIN forge_projects ON project_tags.project_id = forge_projects.project_id
    GROUP BY project_tags.version_id
)
UPDATE project_versions
SET tags = forge_tags_appended.tags
FROM forge_tags_appended
WHERE project_versions.id = forge_tags_appended.version_id;

-- Remove the boolean fields
ALTER TABLE projects
  ALTER COLUMN is_sponge_plugin DROP DEFAULT;
ALTER TABLE projects
  ALTER COLUMN is_forge_mod DROP DEFAULT;

# --- !Downs

-- TODO

drop table project_tags;
