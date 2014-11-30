package se.callista.akka.async

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import se.callista.akka.async.common.{AggregateParams, RoutingSlipParams}
import se.callista.akka.async.pattern.{RoutingSlipActor, AggregateRequest, AggregatorActor}
import spray.can.Http
import spray.http.HttpMethods._
import spray.http._

import scala.concurrent.duration._

class AsyncService(dbLookup: ActorRef) extends Actor with ActorLogging {
  implicit val timeout: Timeout = 3.seconds

  def receive = {
    case _: Http.Connected => sender ? Http.Register(self)

    case request @ HttpRequest(GET, Uri.Path("/aggregate"), headers, _, _) =>
      log.info("{}", headers)
      val aggregator = context.actorOf(AggregatorActor.props(sender(), dbLookup, AggregateParams("http://127.0.0.1:9090")(request)))
      aggregator ! AggregateRequest()

    case request @ HttpRequest(GET, Uri.Path("/routing-slip"), _, _, _) =>
      val routingSlip = context.actorOf(RoutingSlipActor.props(sender(), RoutingSlipParams(request)))
      routingSlip ! 1
  }
}

object AsyncService {
  def props(dbLookup: ActorRef) = Props(classOf[AsyncService], dbLookup)
}

