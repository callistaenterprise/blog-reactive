package se.callista.springmvc.asynch.demo;

import com.ning.http.client.Response;
import org.json.JSONObject;
import se.callista.springmvc.asynch.common.AsyncHttpClientJava8;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class Demo {
	private static final String url = "http://api.openweathermap.org/data/2.5/weather?lat=57.70&lon=11.99";
	private static final Double ABSOLUTE_ZERO = -273.15;

	public CompletableFuture<Double> getTemperature() {
		CompletableFuture<Response> tempJsonFuture = new AsyncHttpClientJava8().execute(url, 1);
		return tempJsonFuture
				.thenApplyAsync(this::extractTemp) //Map
				.thenComposeAsync(this::kelvinToCelcius); //Flatmap
	}

	private Double extractTemp(Response response) {
		String body = getResponseBody(response);
		JSONObject obj = new JSONObject(body);
		return obj.getJSONObject("main").getDouble("temp");
	}

	private CompletableFuture<Double> kelvinToCelcius(Double tempInKelvin) {
		return CompletableFuture.completedFuture(tempInKelvin + ABSOLUTE_ZERO);
	}

	private String getResponseBody(Response response) {
		try {
			return response.getResponseBody();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

//	public static void main(String[] args) {
//		new Demo().getTemperature()
//				.whenComplete((temperature, ex) -> {
//					if (ex == null) {
//						System.out.println(temperature.intValue());
//					} else {
//						System.out.println(ex.getMessage());
//					}
//				})
//				.thenAcceptAsync(temperature -> System.exit(0));
//	}
}
