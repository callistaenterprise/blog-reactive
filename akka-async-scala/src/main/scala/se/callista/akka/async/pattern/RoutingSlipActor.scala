package se.callista.akka.async.pattern

import java.util.concurrent.TimeoutException

import akka.actor._
import se.callista.akka.async.common.{HttpRequestActor, RoutingSlipParams, Settings}
import spray.http.HttpMethods._
import spray.http.MediaTypes._
import spray.http._

import scala.collection.mutable

class RoutingSlipActor(commander: ActorRef, params: RoutingSlipParams) extends Actor with ActorLogging {
  val settings = Settings(context.system)

  val result = mutable.Buffer[HttpResponse]()

  def responseEntity = HttpEntity(`text/plain`, result.map(_.entity.asString).mkString("", "\n", "\n"))

  def handler(step: Int, response: Any, nextStep: (HttpResponse) => Actor.Receive) = {
    val resp = response.asInstanceOf[HttpResponse]
    if(!resp.status.isSuccess) {
      commander ! HttpResponse(status = StatusCodes.InternalServerError, entity = resp.entity)
      context.stop(self)
    }
    result += resp
    val url = getUrl(step, params.qry)
    context.actorOf(HttpRequestActor.props(url)) ! HttpRequest(GET, url)
    context.become(nextStep(resp))
  }

  def errorHandler(t: Throwable) = t match {
    case t: TimeoutException =>
      commander ! HttpResponse(
        status = StatusCodes.GatewayTimeout,
        entity = HttpEntity(s"Request failed due to service provider not responding within ${settings.REQ_TMO}. Url: ${t.getMessage}"))
      context.stop(self)

    case _ =>
      commander ! HttpResponse(status = StatusCodes.InternalServerError)
      context.stop(self)
  }

  override def receive: Receive = {
    case _ =>
      val url = getUrl(1, params.qry)
      context.actorOf(HttpRequestActor.props(url)) ! HttpRequest(GET, url)
      context.become(waitForFirstResponse)
  }

  def waitForFirstResponse: Receive = {
    case Status.Success(response) =>
      handler(2, response,
        r => if(r.status.isSuccess) waitForThirdResponse else waitForSecondResponse)

    case Status.Failure(t) => errorHandler(t)
  }

  def waitForSecondResponse: Receive = {
    case Status.Success(response) =>
      handler(3, response, _ => waitForFourthResponse)
    case Status.Failure(t) => errorHandler(t)
  }

  def waitForThirdResponse: Receive = {
    case Status.Success(response) =>
      handler(4, response, _ => waitForFourthResponse)
    case Status.Failure(t) => errorHandler(t)
  }

  def waitForFourthResponse: Receive = {
    case Status.Success(response) =>
      handler(5, response, _ => last)
    case Status.Failure(t) => errorHandler(t)
  }

  def last: Receive = {
    case Status.Success(response) =>
      result += response.asInstanceOf[HttpResponse]
      commander ! HttpResponse(entity = responseEntity)
      context.stop(self)
    case Status.Failure(t) => errorHandler(t)
  }

  private def getUrl(processingStepNo: Int, qry: Option[String]): Uri = {
    val sleeptimeMs: String = (100 * processingStepNo).toString
    val query = Map("minMs" -> sleeptimeMs, "maxMs" -> sleeptimeMs)

    Uri(settings.SP_NON_BLOCKING_URL).withQuery(query ++ qry.map("qry" -> _))
  }
}

object RoutingSlipActor {
  def props(commander: ActorRef, params: RoutingSlipParams) = Props(classOf[RoutingSlipActor], commander, params)
}
