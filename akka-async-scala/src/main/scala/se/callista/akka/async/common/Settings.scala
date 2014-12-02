package se.callista.akka.async.common

import akka.actor.{ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}
import com.typesafe.config.Config

/**
 *
 */
class SettingsImpl(config: Config) extends Extension {
  lazy val SP_NON_BLOCKING_URL: String = config.getString("routing-slip.url")
  lazy val AGG_TMO_MS: Int = config.getInt("aggregator.timeoutMs")
  lazy val REQ_TMO: String = config.getString("spray.can.client.request-timeout")
}

object Settings extends ExtensionId[SettingsImpl] with ExtensionIdProvider {

  override def createExtension(system: ExtendedActorSystem): SettingsImpl =
    new SettingsImpl(system.settings.config)

  override def lookup() = Settings
}