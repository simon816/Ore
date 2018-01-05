# File path fixing for Ore
#
# Ore before ecd8100420e9095500ff409ac738948b25c3eed6 placed
# some versions in an incorrect location. This script will move
# those files to the location they should be. Plugins that don't
# contain an mcmod.info file can't be moved.
#
# Dependencies: psycopg2

import psycopg2
import zipfile
import json
import os.path

# variables
dbname = "ore"
user = "postgres"
host = "localhost"
password = "password"
main_directory = "/home/ore/files/plugins/" # with slash at the end

def plugin_metadata(path):
  jar = zipfile.ZipFile(path, 'r')
  mcmodinfo = jar.read("mcmod.info")
  data = json.loads(mcmodinfo.decode("utf-8"))[0]
  return data

# Connec to the DB
connection = psycopg2.connect(dbname=dbname, user=user, host=host, password=password)
cursor = connection.cursor()

old_project_names = {}

cursor.execute("""SELECT project_id, file_name FROM project_versions ORDER BY id""")
project_versions = cursor.fetchall()

for project_version in project_versions:
  project_id = project_version[0]
  cursor.execute("""SELECT name, owner_id FROM projects WHERE id = """ + str(project_id))
  project = cursor.fetchall()[0]

  cursor.execute("""SELECT name FROM users WHERE id = """ + str(project[1]))
  user = cursor.fetchall()[0]

  if project_id in old_project_names:
    old_name = old_project_names[project_id]
    new_name = project[0]
    if old_name != new_name:
      old_path = main_directory + user[0] + "/" + old_name + "/" + project_version[1]
      new_path = main_directory + user[0] + "/" + new_name + "/" + project_version[1]
      if os.path.isfile(old_path):
        print("Old: " + old_path + "\nNew: " + new_path)
        os.rename(old_path, new_path)
  else:
    print("Caching project id: " + str(project_id) + " (" + project[0] + ")")
    file = main_directory + user[0] + "/" + project[0] + "/" + project_version[1]
    try:
      metadata = plugin_metadata(file)
      old_project_names[project_id] = metadata["name"]
    except:
      print("Project " + file + " (" + str(project_id) + " " + project[0] + ") doesn't contain mcmod.info")

cursor.close()
connection.close()
