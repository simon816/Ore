package db.impl

import db.impl.access.{OrganizationBase, ProjectBase, UserBase}
import db.impl.model.OreModel
import db.{Model, ModelProcessor, ModelService}
import discourse.DiscourseApi
import ore.OreConfig

/**
  * A ModelProcessor that performs Ore specific model processing.
  *
  * @param service  ModelService
  * @param config   Ore config
  */
class OreModelProcessor(service: ModelService,
                        users: UserBase,
                        projects: ProjectBase,
                        organizations: OrganizationBase,
                        config: OreConfig,
                        forums: DiscourseApi)
                        extends ModelProcessor(service) {

  override def process[M <: Model](model: M) = {
    super.process(model) match {
      case oreModel: OreModel =>
        oreModel.userBase = this.users
        oreModel.projectBase = this.projects
        oreModel.organizationBase = this.organizations
        oreModel.config = this.config
        oreModel.forums = this.forums
      case _ =>
    }
    model
  }
}
