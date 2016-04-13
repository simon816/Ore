package util

import java.nio.file.Files

import play.api.Play
import play.api.Play.current
import play.api.Play.{configuration => config}

object Git {

  private val repoUrl = config.getString("ore.git").get
  private val branchFile = Play.application.path.toPath.resolve(".git/refs/heads/master")
  private val refLength = 7

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
