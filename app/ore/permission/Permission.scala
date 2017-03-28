package ore.permission

import ore.permission.role.{Absolute, Limited, Standard, Trust}

/**
  * Represents a permission for a user to do something in the application.
  */
sealed trait Permission { def trust: Trust }
case object EditChannels        extends Permission { val trust = Standard }
case object EditPages           extends Permission { val trust = Limited  }
case object EditSettings        extends Permission { val trust = Absolute }
case object EditVersions        extends Permission { val trust = Standard }
case object HideProjects        extends Permission { val trust = Standard }
case object ReviewFlags         extends Permission { val trust = Standard }
case object ReviewProjects      extends Permission { val trust = Standard }
case object ViewHealth          extends Permission { val trust = Standard }
case object ViewLogs            extends Permission { val trust = Standard }
case object ResetOre            extends Permission { val trust = Absolute }
case object SeedOre             extends Permission { val trust = Absolute }
case object MigrateOre          extends Permission { val trust = Absolute }
case object CreateProject       extends Permission { val trust = Absolute }
case object PostAsOrganization  extends Permission { val trust = Standard }
case object EditApiKeys       extends Permission { val trust = Absolute }
