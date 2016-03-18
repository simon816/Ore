package plugin

/**
  * Exception thrown when an uploaded PluginFile is invalid.
  */
case class InvalidPluginFileException(message: String = null, cause: Throwable = null)
  extends Exception(message, cause)

