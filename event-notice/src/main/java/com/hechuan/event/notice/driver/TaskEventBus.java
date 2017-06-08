package com.hechuan.event.notice.driver;

import com.google.common.base.MoreObjects;
import com.google.common.util.concurrent.MoreExecutors;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * task事件总线
 * 
 * @author hechuan
 *
 * @created 2017年6月8日
 *
 * @version 1.0.0
 */
@Component
public class TaskEventBus {

	/** 未知事件相关 */
	public static final String DEAD_EVENT = "DEAD";

	private final String identifier;
	private final Executor executor;
	private final SubscriberExceptionHandler exceptionHandler;

	private final SubscriberRegistry subscribers = new SubscriberRegistry(this);
	
	private final Dispatcher dispatcher;

	
	public TaskEventBus() {
		this("default");
	}

	
	public TaskEventBus(String identifier) {
		this(identifier, MoreExecutors.directExecutor(), Dispatcher.perThreadDispatchQueue(), LoggingHandler.INSTANCE);
	}

	public TaskEventBus(SubscriberExceptionHandler exceptionHandler) {
		this("default", MoreExecutors.directExecutor(), Dispatcher.perThreadDispatchQueue(), exceptionHandler);
	}

	TaskEventBus(String identifier, Executor executor, Dispatcher dispatcher,
			SubscriberExceptionHandler exceptionHandler) {
		this.identifier = checkNotNull(identifier);
		this.executor = checkNotNull(executor);
		this.dispatcher = checkNotNull(dispatcher);
		this.exceptionHandler = checkNotNull(exceptionHandler);
	}

	public final String identifier() {
		return identifier;
	}

	final Executor executor() {
		return executor;
	}

	public void handleSubscriberException(Throwable e, SubscriberExceptionContext context) {
		checkNotNull(e);
		checkNotNull(context);
		exceptionHandler.handleException(e, context);
	}

	
	public void register(Object object) {
		subscribers.register(object);
	}

	
	public void unregister(Object object) {
		subscribers.unregister(object);
	}

	
	public void post(PostEvent postEvent) {
		Iterator<Subscriber> eventSubscribers = subscribers.getSubscribers(postEvent);
		if (eventSubscribers.hasNext()) {
			dispatcher.dispatch(postEvent.getEvent(), eventSubscribers);
		} else if (!(postEvent.getEvent() instanceof DeadEvent)) {
			throw new RuntimeException("该事件无任何监听者处理");
		}
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).addValue(identifier).toString();
	}

	/**
	 * Simple logging handler for subscriber exceptions.
	 */
	static final class LoggingHandler implements SubscriberExceptionHandler {
		static final LoggingHandler INSTANCE = new LoggingHandler();

		@Override
		public void handleException(Throwable exception, SubscriberExceptionContext context) {
			Logger logger = logger(context);
			if (logger.isLoggable(Level.SEVERE)) {
				logger.log(Level.SEVERE, message(context), exception);
			}
		}

		private static Logger logger(SubscriberExceptionContext context) {
			return Logger.getLogger(TaskEventBus.class.getName() + "." + context.getEventBus().identifier());
		}

		private static String message(SubscriberExceptionContext context) {
			Method method = context.getSubscriberMethod();
			return "Exception thrown by subscriber method " + method.getName() + '('
					+ method.getParameterTypes()[0].getName() + ')' + " on subscriber " + context.getSubscriber()
					+ " when dispatching event: " + context.getEvent();
		}
	}

}
