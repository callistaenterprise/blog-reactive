package controllers

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws._
import play.api.mvc._
import util.{Contexts, ConfigUtil, DbLookup}

import scala.collection.JavaConverters._
import scala.concurrent.Future.sequence
import scala.concurrent.{Future, TimeoutException}

object Application extends Controller with ConfigUtil {

  /**
   *
   */
  def aggregate(dbLookupMs: Int, dbHits: Int, minMs: Int, maxMs: Int) = Action.async {

    val dbLookup = new DbLookup(dbLookupMs, dbHits)

    val urlsF = Future{
      dbLookup.lookupUrlsInDb(SP_NON_BLOCKING_URL, minMs, maxMs).asScala
    }(Contexts.simpleDbLookups)

    urlsF.flatMap { urls =>
      sequence(
        urls.zipWithIndex.map { case (url, index) =>
          WS
            .url(url)
            .withRequestTimeout(TIMEOUT_MS)
            .get()
            .map(r => Option(r.body))
            .recover {
              case _: TimeoutException => None
              case t => Option(s"Request #$index failed due to error: $t")
          }
        }
      ).map(v => Ok(v.flatten.mkString("", "\n", "\n")))
    }
  }

  /**
   *
   */
  def routingSlip = Action.async {

    def processResult(s: String) = true

    def getUrl(processingStepNo: Int) = {
      val sleeptimeMs = 100 * processingStepNo
      s"$SP_NON_BLOCKING_URL?minMs=$sleeptimeMs&maxMs=$sleeptimeMs"
    }

    def doAsyncCall(num: Int) = WS.url(getUrl(num)).get().map(r => (r.body, processResult(r.body)))

    val result =
      for {
        (r1, next) <- doAsyncCall(1)
        (r2, next) <- doAsyncCall(2)
        (r3, next) <- doAsyncCall(if (next) 4 else 3)
        (r4, next) <- doAsyncCall(5)
      } yield List(r1, r2, r3, r4)

    result.map(v => Ok(v.mkString("", "\n", "\n")))
  }
}