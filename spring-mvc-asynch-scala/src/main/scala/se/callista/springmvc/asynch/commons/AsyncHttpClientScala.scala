package se.callista.springmvc.asynch.commons

import com.ning.http.client.{AsyncCompletionHandler, AsyncHttpClient, Response}
import org.slf4j.LoggerFactory

import scala.concurrent.{Future, Promise}

/**
 * Created by anders on 14-11-04.
 */
object AsyncHttpClientScala {

	private val logger = LoggerFactory.getLogger(AsyncHttpClientScala.getClass)
	private val asyncHttpClient = new AsyncHttpClient();

	def get(url: String): Future[Response] = {
		val result = Promise[Response]

		asyncHttpClient.prepareGet(url).execute(new AsyncCompletionHandler[Response] {
			override def onCompleted(response: Response) = {
				result.success(response)
				response
			}
			override def onThrowable(t: Throwable) = {
				result.failure(t)
			}
		})
		result.future
	}

}
