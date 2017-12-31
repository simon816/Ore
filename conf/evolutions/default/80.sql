# --- !Ups

-- create the tags table
CREATE TABLE project_tags (
  id          SERIAL,
  version_ids BIGINT [],
  name        VARCHAR(255),
  data        VARCHAR(255),
  color       INT
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
      project_versions.id AS version_id,
      project_versions.created_at,
      -- split the sponge version string and get the version number
      split_part(
      -- get the dependency that starts with "spongeapi:"
          (ARRAY(SELECT *
                 FROM unnest(dependencies) AS s(dependency_string)
                 WHERE dependency_string ILIKE 'spongeapi:%')) [1],
          ':', 2
      )                   AS data
    FROM project_versions
      INNER JOIN sponge_projects ON project_versions.project_id = sponge_projects.project_id
)
  , sponge_tag_info_merged AS (
    SELECT
      array_agg(version_id) AS version_ids,
      data
    FROM sponge_tag_info
    GROUP BY data
)
INSERT INTO project_tags (version_ids, name, data, color)
  SELECT
    version_ids,
    'Sponge' AS name,
    data,
    -- TODO Correct Sponge Color (This is Orange in ore/Colors.scala)
    13       AS color
  FROM sponge_tag_info_merged;

-- add the tags into the projects table
WITH sponge_projects AS (
  -- get the projects that have is_sponge_plugin set to true
    SELECT
      id AS project_id,
      created_at
    FROM projects
    WHERE is_sponge_plugin = TRUE
)
  , sponge_project_versions AS (
  -- get all the versions that are sponge plugins
    SELECT project_versions.id AS version_id
    FROM project_versions
    WHERE project_versions.project_id IN (SELECT project_id
                                          FROM sponge_projects)
)
  , sponge_tags_appended AS (
    SELECT
      project_tags.version_ids,
      array_agg(DISTINCT project_tags.id) AS tags
    FROM project_tags
      JOIN sponge_project_versions ON sponge_project_versions.version_id = ANY (project_tags.version_ids)
    GROUP BY project_tags.version_ids
)
UPDATE project_versions
SET tags = sponge_tags_appended.tags
FROM sponge_tags_appended
WHERE project_versions.id = ANY (sponge_tags_appended.version_ids);

-- migrate the forge versions to the tags table
WITH forge_projects AS (
  -- get the projects that have is_forge_mod set to true
    SELECT
      id AS project_id,
      created_at
    FROM projects
    WHERE is_forge_mod = TRUE
)
  , forge_tag_info AS (
    SELECT
      project_versions.project_id,
      project_versions.id AS version_id,
      project_versions.created_at,
      -- split the forge version string and get the version number
      split_part(
      -- get the dependency that starts with "Forge:"
          (ARRAY(SELECT *
                 FROM unnest(dependencies) AS s(dependency_string)
                 WHERE dependency_string ILIKE 'Forge:%')) [1],
          ':', 2
      )                   AS data
    FROM project_versions
      INNER JOIN forge_projects ON project_versions.project_id = forge_projects.project_id
)
  , forge_tag_info_merged AS (
    SELECT
      array_agg(version_id) AS version_ids,
      data
    FROM forge_tag_info
    GROUP BY data
)
INSERT INTO project_tags (version_ids, name, data, color)
  SELECT
    version_ids,
    'Forge' AS name,
    data,
    -- TODO Correct Forge Color (This is Red in ore/Colors.scala)
    14      AS color
  FROM forge_tag_info_merged;

-- add the tags into the projects table
WITH forge_projects AS (
  -- get the projects that have is_forge_mod set to true
    SELECT
      id AS project_id,
      created_at
    FROM projects
    WHERE is_forge_mod = TRUE
)
  , forge_project_versions AS (
  -- get all the versions that are forge mods
    SELECT project_versions.id AS version_id
    FROM project_versions
    WHERE project_versions.project_id IN (SELECT project_id
                                          FROM forge_projects)
)
  , forge_tags_appended AS (
    SELECT
      project_tags.version_ids,
      array_agg(DISTINCT project_tags.id) AS tags
    FROM project_tags
      JOIN forge_project_versions ON version_id = ANY (project_tags.version_ids)
    GROUP BY project_tags.version_ids
)
UPDATE project_versions
SET tags = forge_tags_appended.tags
FROM forge_tags_appended
WHERE project_versions.id = ANY (forge_tags_appended.version_ids);

-- Remove the boolean fields
ALTER TABLE projects
  DROP COLUMN is_sponge_plugin;
ALTER TABLE projects
  DROP COLUMN is_forge_mod;

# --- !Downs

-- TODO

-- add the tags column
ALTER TABLE project_versions
  ADD COLUMN is_sponge_plugin BOOLEAN DEFAULT FALSE;
ALTER TABLE project_versions
  ADD COLUMN is_forge_mod BOOLEAN DEFAULT FALSE;

-- when sponge_

DROP TABLE project_tags;
