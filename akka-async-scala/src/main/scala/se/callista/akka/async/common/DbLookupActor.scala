package se.callista.akka.async.common

import akka.actor.{Actor, ActorLogging}
import spray.http.Uri

/**
 *
 */
case class LookupRequest(params: AggregateParams)

case class LookupResponse(urls: List[Uri])

class DbLookupWorker extends Actor with ActorLogging {
  def receive = {
    case LookupRequest(p) =>
      log.debug("**** " + Thread.currentThread().getName)
      Thread.sleep(p.dbLookupMs)
      sender() ! LookupResponse(
        (1 to p.dbHits map
          (num => Uri(p.baseUrl)
            .withQuery("minMs" -> p.minMs.toString, "maxMs" -> p.maxMs.toString))).toList
      )
  }
}
