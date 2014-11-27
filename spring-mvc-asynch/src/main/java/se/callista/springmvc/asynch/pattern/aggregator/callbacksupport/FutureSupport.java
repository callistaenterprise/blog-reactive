package se.callista.springmvc.asynch.pattern.aggregator.callbacksupport;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class FutureSupport {

	private FutureSupport() { }

	public static <T> CompletableFuture<List<T>> sequence(List<CompletableFuture<T>> futures) {
		CompletableFuture<Void> allDoneFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
		return allDoneFuture.thenApply(v -> joinFutures(futures));
	}

	public static <T> CompletableFuture<List<T>> sequence(CompletableFuture<T>... futures) {
		return sequence(Arrays.asList(futures));
	}

	private static <T> List<T> joinFutures(List<CompletableFuture<T>> futures) {
		return futures.stream().map(CompletableFuture::join).collect(Collectors.<T>toList());
	}

//	public  <T> T throwNewException(Throwable cause, Class<Throwable> throwableClass, String... args)  {
//		throwableClass.newInstance()
//		throw new AsyncCallException(url, t);
//	}

}
