package se.callista.springmvc.asynch.pattern.aggregator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

public class TimeoutDefault {

	private static final Logger logger = LoggerFactory.getLogger(TimeoutDefault.class);
	private static final Timer timer = new Timer();

	public static <T> CompletableFuture<Optional<T>> with(int ms) {
		final CompletableFuture<T> result = new CompletableFuture<>();

		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				result.complete((T) Optional.empty());
			}
		}, ms);

		return (CompletableFuture<Optional<T>>) result;
	}
}


