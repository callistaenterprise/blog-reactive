package se.callista.springmvc.asynch.demo

import com.ning.http.client.Response
import org.json4s._
import org.json4s.native.JsonMethods._
import se.callista.springmvc.asynch.commons.lambdasupport.AsyncHttpClientScala
import se.callista.springmvc.asynch.demo.Demo.getTemperature

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object Demo {

	val url = "http://api.openweathermap.org/data/2.5/weather?lat=57.70&lon=11.99"
	val ABSOLUTE_ZERO = -273.15

	def getTemperature() = {
		val tempJsonFuture: Future[Response] = AsyncHttpClientScala.execute(url)
		tempJsonFuture map extractTemp flatMap kelvinToCelcius
	}

	def kelvinToCelcius(tempInKelvin: Double): Future[Double] = Future {
		tempInKelvin + ABSOLUTE_ZERO
	}

	def extractTemp(response: Response): Double = {
		val body = response getResponseBody
		val json = parse(body)
		compact(render(json \ "main" \ "temp")).toDouble
	}
}



object DemoApplication {
	def main(args: Array[String]) {

		getTemperature andThen {
			case Success(f) => println(f.toInt)
			case Failure(t) => println(t)
		} andThen {
			case _ => System.exit(0)
		}

	}
}
