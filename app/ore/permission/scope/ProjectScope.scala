package ore.permission.scope

import db.orm.model.ProjectOwner

/**
  * Represents a scope of a certain Project.
  */
trait ProjectScope extends Scope with ProjectOwner {
  override val parent: Option[Scope] = Some(GlobalScope)
}
