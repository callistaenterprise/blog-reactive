package util

import play.api.libs.concurrent.Akka

import scala.concurrent.ExecutionContext

import play.api.Play.current

/**
 *
 */
object Contexts {
  implicit val simpleDbLookups: ExecutionContext = Akka.system.dispatchers.lookup("contexts.simple-db-lookups")
}