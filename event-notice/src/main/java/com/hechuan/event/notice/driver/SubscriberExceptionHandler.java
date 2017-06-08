package com.hechuan.event.notice.driver;

/**
 * 异常处理接口
 * 
 * @author hechuan
 *
 * @created 2017年6月8日
 *
 * @version 1.0.0
 */
public interface SubscriberExceptionHandler {
	/**
	 * Handles exceptions thrown by subscribers.
	 */
	void handleException(Throwable exception, SubscriberExceptionContext context);
}
