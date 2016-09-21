package ore

import javax.inject.Inject

import play.api.Environment

/**
  * Helper class for getting commonly used Paths.
  */
final class OreEnv @Inject()(val env: Environment) {

  lazy val root    =  this.env.rootPath.toPath
  lazy val public  =  this.root.resolve("public")
  lazy val conf    =  this.root.resolve("conf")
  lazy val uploads =  this.root.resolve("uploads")
  lazy val plugins =  this.uploads.resolve("plugins")
  lazy val tmp     =  this.uploads.resolve("tmp")

}
