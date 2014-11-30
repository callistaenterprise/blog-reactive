package se.callista.akka.async.common

import java.util.concurrent.TimeoutException

import akka.actor._
import akka.io.IO
import spray.can.Http
import spray.http.{HttpRequest, HttpResponse, Timedout, Uri}

/**
 *
 */
class HttpRequestActor(uri: Uri) extends Actor with ActorLogging {

  import context.system

  val host = uri.authority.host.address
  val port = uri.authority.port

  def receive: Receive = {
    case request: HttpRequest =>
      IO(Http) ! Http.Connect(host, port)
      context.become(connecting(sender(), request))
  }

  def connecting(cmd: ActorRef, request: HttpRequest): Receive = {
    case _: Http.Connected =>
      sender() ! request
      context.become(waitingForResponse(cmd))

    case Http.CommandFailed(Http.Connect(address, _, _, _, _)) =>
      cmd ! Status.Failure(new RuntimeException("Connection error"))
  }

  def waitingForResponse(cmd: ActorRef): Receive = {
    case response @ HttpResponse(status, entity, _, _) =>
      sender() ! Http.Close
      context.become(waitingForClose(cmd, response))

    case ev @ Http.SendFailed(req) =>
      cmd ! Status.Failure(new RuntimeException("Request error"))
      context.stop(self)

    case ev @ Timedout(_) =>
      cmd ! Status.Failure(new TimeoutException(uri.toString()))
      context.stop(self)
  }

  def waitingForClose(cmd: ActorRef, response: HttpResponse): Receive = {
    case ev: Http.ConnectionClosed =>
      cmd ! Status.Success(response)
      context.stop(self)

    case Http.CommandFailed(Http.Close) =>
      cmd ! Status.Failure(new RuntimeException("Connection close error"))
      context.stop(self)
  }
}

object HttpRequestActor {
  def props(uri: Uri) = Props(classOf[HttpRequestActor], uri)
}

