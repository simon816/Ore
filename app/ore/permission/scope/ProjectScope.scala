package ore.permission.scope

/**
  * Represents a scope of a certain Project.
  *
  * @param projectId Project ID of scope
  */
case class ProjectScope(projectId: Int) extends Scope { override val parent: Option[Scope] = Some(GlobalScope) }
