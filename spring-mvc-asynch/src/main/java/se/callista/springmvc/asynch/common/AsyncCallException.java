package se.callista.springmvc.asynch.common;

/**
 *
 */
public class AsyncCallException extends RuntimeException {
	private String url;

	public AsyncCallException(String url, Throwable cause) {
		super(cause);
		this.url = url;
	}

	public String getUrl() {
		return url;
	}
}

