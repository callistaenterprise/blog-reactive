package se.callista.springmvc.asynch.commons.lambdasupport

import com.ning.http.client.{AsyncCompletionHandler, Response, AsyncHttpClient}
import org.slf4j.LoggerFactory

import scala.concurrent.{Promise, Future}

/**
 * Created by anders on 14-11-04.
 */
object AsyncHttpClientScala {

	private val logger = LoggerFactory.getLogger(AsyncHttpClientScala.getClass)
	private val asyncHttpClient = new AsyncHttpClient();

	def execute(url: String): Future[Response] = {
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
