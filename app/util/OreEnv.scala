package util

import javax.inject.Inject

import play.api.Environment

/**
  * Helper class for getting commonly used Paths.
  */
final class OreEnv @Inject()(val env: Environment) {

  lazy val root    =  env.rootPath.toPath
  lazy val conf    =  root.resolve("conf")
  lazy val uploads =  root.resolve("uploads")
  lazy val plugins =  uploads.resolve("plugins")
  lazy val tmp     =  uploads.resolve("tmp")

}
