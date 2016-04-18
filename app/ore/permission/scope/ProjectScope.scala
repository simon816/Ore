package ore.permission.scope

/**
  * Represents a scope of a certain Project.
  */
trait ProjectScope extends Scope {
  def projectId: Int
  override val parent: Option[Scope] = Some(GlobalScope)
}
