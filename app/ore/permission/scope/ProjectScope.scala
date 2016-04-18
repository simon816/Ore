package ore.permission.scope

/**
  * Represents a scope of a certain Project.
  */
trait ProjectScope extends Scope {
  val projectId: Int
  override val parent: Option[Scope] = Some(GlobalScope)
}
