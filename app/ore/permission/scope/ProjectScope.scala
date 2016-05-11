package ore.permission.scope

import ore.ProjectOwned

/**
  * Represents a scope of a certain Project.
  */
trait ProjectScope extends Scope with ProjectOwned {
  override val parent: Option[Scope] = Some(GlobalScope)
}
