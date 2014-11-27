package util

import play.api.Play

/**
 *
 */
trait ConfigUtil {
  lazy val SP_NON_BLOCKING_URL = mandatory("sp.non_blocking.url")
  lazy val TIMEOUT_MS = mandatory("aggregator.timeoutMs").toInt

  private def mandatory(s: String) = Play.current.configuration.getString(s).getOrElse(sys.error(s"$s not configured!"))
 }
