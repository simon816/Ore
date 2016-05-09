package forums

class DisabledDiscourseApi extends DiscourseApi {
  override val Auth = null
  override val Users = DiscourseUsers.Disabled
  override val Embed = DiscourseEmbed.Disabled
}
