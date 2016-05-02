package ore.permission.scope

import ore.ProjectOwner

/**
  * Represents a scope of a certain Project.
  */
trait ProjectScope extends Scope with ProjectOwner {
  override val parent: Option[Scope] = Some(GlobalScope)
}
