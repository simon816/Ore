package ore.user

import ore.Colors
import Colors.Color
import ore.user.Donor._
import ore.user.UserRoles.UserRole

/**
  * Represents a Sponge donor.
  *
  * @param i            Index
  * @param id           ID of role
  * @param title        Title to display
  * @param color        Color to display
  */
class Donor(override val   i: Int,
            override val   id: Int,
            override val   title: String,
            override val   color: Color)
            extends        UserRole(i, id, TrustLevel, title, color) {

}

object Donor {

  /**
    * The trust level all donors posess.
    */
  val TrustLevel: Int = 1

}
