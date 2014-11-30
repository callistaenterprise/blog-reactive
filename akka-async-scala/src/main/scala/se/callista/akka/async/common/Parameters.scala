package se.callista.akka.async.common

import spray.http.{HttpHeader, HttpRequest}

trait Params {
  def queryParamInt(name: String)(implicit req: HttpRequest) =
    req.uri.query.get(name).map(_.toInt)

  def queryParamStr(name: String)(implicit req: HttpRequest) =
    req.uri.query.get(name)

}

case class AggregateParams(baseUrl: String, accept: List[HttpHeader], minMs: Int, maxMs: Int, dbLookupMs: Int, dbHits: Int)

case object AggregateParams extends Params {

  def apply(baseUrl: String)(implicit req: HttpRequest): AggregateParams = AggregateParams(
    baseUrl = baseUrl,
    accept = req.headers.filter(_.lowercaseName == "accept"),
    dbLookupMs = queryParamInt("dbLookupMs") getOrElse 0,
    dbHits = queryParamInt("dbHits") getOrElse 3,
    minMs = queryParamInt("minMs") getOrElse 0,
    maxMs = queryParamInt("maxMs") getOrElse 0
  )
}

case class RoutingSlipParams(qry: Option[String])

object RoutingSlipParams extends Params {
  def apply(implicit req: HttpRequest): RoutingSlipParams = RoutingSlipParams(
    qry = queryParamStr("qry")
  )
}
