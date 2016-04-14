package util

import java.nio.file.Files

import play.api.Play
import play.api.Play.{configuration => config, current}

/**
  * Helper to retrieve current Git ref.
  */
object Git {

  private val repoUrl = config.getString("ore.git.url").get
  private val branchFile = Play.application.path.toPath.resolve(config.getString("ore.git.ref").get)
  private val refLength = config.getInt("ore.git.ref-len").get

  /**
    * Returns a shortened ref of the current git ref and the URL to the
    * relevant commit if exists.
    *
    * @return Current ref on master if exists, tuple of empty strings otherwise
    */
  def ref: (String, String) = {
    if (Files.exists(branchFile)) {
      val ref = new String(Files.readAllBytes(branchFile))
      val shortRef = ref.substring(0, refLength)
      (shortRef, repoUrl + "/commit/" + ref)
    } else {
      ("", "")
    }
  }

}
