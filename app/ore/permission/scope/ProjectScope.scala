package ore.permission.scope

import models.project.Project

/**
  * Represents a scope of a certain Project.
  */
trait ProjectScope extends Scope {

  /**
    * ID of [[models.project.Project]] this scope represents.
    *
    * @return ID of project
    */
  def projectId: Int

  /**
    * Returns the [[Project]] of this [[Scope]].
    *
    * @return Project of scope
    */
  def project: Project = Project.withId(this.projectId).get

  override val parent: Option[Scope] = Some(GlobalScope)

}
