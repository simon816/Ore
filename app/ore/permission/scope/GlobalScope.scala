package ore.permission.scope

/**
  * Represents the root Scope of the application.
  */
object GlobalScope extends Scope with ScopeSubject { override val scope: Scope = this }
