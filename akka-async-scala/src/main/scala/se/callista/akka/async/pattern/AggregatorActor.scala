package se.callista.akka.async.pattern

case class AggregateRequest()

case object Tmo

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Status}
import se.callista.akka.async.common.{AggregateParams, HttpRequestActor, LookupRequest, LookupResponse}
import spray.http.HttpMethods._
import spray.http.MediaTypes._
import spray.http.{HttpEntity, HttpRequest, HttpResponse}

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class AggregatorActor(commander: ActorRef, dbLookup: ActorRef, params: AggregateParams) extends Actor with ActorLogging {

  val result = mutable.Buffer[HttpResponse]()
  var expectedResults = 0

  def responseEntity = HttpEntity(`text/plain`, result.map(_.entity.asString).mkString("", "\n", "\n"))

  def receive = {

    case _: AggregateRequest => // Start here
      context.system.scheduler.scheduleOnce(3.seconds, self, Tmo)
      dbLookup ! LookupRequest(params)

    case LookupResponse(uris) => // Db response delivered
      expectedResults = uris.length
      uris foreach (uri => context.actorOf(HttpRequestActor.props(uri)) ! HttpRequest(GET, uri, params.accept))

    case Status.Success(response) => // One response delivered
      result += response.asInstanceOf[HttpResponse]
      if (result.length == expectedResults) {
        commander ! HttpResponse(entity = responseEntity)
        context.stop(self)
      }

    case Tmo => // Timeout reached
      commander ! HttpResponse(entity = responseEntity)
      context.stop(self)

    case Status.Failure(t) =>
      commander ! HttpResponse(
        entity = HttpEntity(`text/html`, "Error")
      )
      context.stop(self)
  }
}

object AggregatorActor {
  def props(ref: ActorRef, dbLookup: ActorRef, params: AggregateParams) =
    Props(classOf[AggregatorActor], ref, dbLookup, params)
}
