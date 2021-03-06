package models.api

import db.impl.schema.ProjectApiKeyTable
import db.{DbRef, DefaultModelCompanion, ModelQuery}
import models.project.Project
import ore.project.ProjectOwned
import ore.rest.ProjectApiKeyType

import slick.lifted.TableQuery

case class ProjectApiKey(
    projectId: DbRef[Project],
    keyType: ProjectApiKeyType,
    value: String
)
object ProjectApiKey extends DefaultModelCompanion[ProjectApiKey, ProjectApiKeyTable](TableQuery[ProjectApiKeyTable]) {
  implicit val query: ModelQuery[ProjectApiKey] =
    ModelQuery.from(this)

  implicit val isProjectOwned: ProjectOwned[ProjectApiKey] = (a: ProjectApiKey) => a.projectId
}
