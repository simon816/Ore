package ore.permission.role

/**
  * Represents a level of trust within the application.
  */
sealed trait Trust extends Ordered[Trust] {
  def level: Int
  override def compare(that: Trust) = this.level - that.level
}

case object Default   extends Trust { override val level = 0 }
case object Limited   extends Trust { override val level = 1 }
case object Standard  extends Trust { override val level = 2 }
case object Absolute  extends Trust { override val level = 3 }
