package ore.permission

import ore.permission.role.{Limited, Standard, Absolute, Trust}

/**
  * Represents a permission for a user to do something in the application.
  */
sealed trait Permission { def trust: Trust }
case object EditChannels    extends Permission { val trust = Absolute }
case object EditPages       extends Permission { val trust = Limited  }
case object EditSettings    extends Permission { val trust = Absolute }
case object EditVersions    extends Permission { val trust = Standard }
case object CreateVersions  extends Permission { val trust = Absolute }
