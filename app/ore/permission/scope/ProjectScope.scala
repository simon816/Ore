package ore.permission.scope

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

  override val parent: Option[Scope] = Some(GlobalScope)

}
