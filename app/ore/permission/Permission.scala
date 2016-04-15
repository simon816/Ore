package ore.permission

import ore.permission.role.{Absolute, Trust}

/**
  * Represents a permission for a user to do something in the application.
  */
sealed trait Permission { def trust: Trust }
case object EditChannels   extends Permission { val trust = Absolute }
case object ViewSettings   extends Permission { val trust = Absolute }
case object DeleteProjects extends Permission { val trust = Absolute }
