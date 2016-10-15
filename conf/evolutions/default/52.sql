# --- !Ups

update projects set recommended_version_id = -1 where recommended_version_id is null;
update projects set topic_id = -1 where topic_id is null;
update projects set post_id = -1 where post_id is null;
