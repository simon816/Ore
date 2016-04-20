package util.form

case class ProjectRoleSetBuilder(override val users: List[Int],
                                 override val roles: List[String])
                                 extends TraitProjectRoleSetBuilder
