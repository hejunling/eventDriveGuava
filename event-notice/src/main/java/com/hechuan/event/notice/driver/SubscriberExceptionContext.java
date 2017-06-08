package com.hechuan.event.notice.driver;

import com.google.common.eventbus.EventBus;

import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * 异常容器类
 * 
 * @author hechuan
 *
 * @created 2017年6月8日
 *
 * @version 1.0.0
 */
public class SubscriberExceptionContext {
	private final TaskEventBus eventBus;
	private final Object event;
	private final Object subscriber;
	private final Method subscriberMethod;

	/**
	 * @param eventBus
	 *            The {@link TaskEventBus} that handled the event and the
	 *            subscriber. Useful for broadcasting a a new event based on the
	 *            error.
	 * @param event
	 *            The event object that caused the subscriber to throw.
	 * @param subscriber
	 *            The source subscriber context.
	 * @param subscriberMethod
	 *            the subscribed method.
	 */
	SubscriberExceptionContext(TaskEventBus eventBus, Object event, Object subscriber, Method subscriberMethod) {
		this.eventBus = checkNotNull(eventBus);
		this.event = checkNotNull(event);
		this.subscriber = checkNotNull(subscriber);
		this.subscriberMethod = checkNotNull(subscriberMethod);
	}

	/**
	 * @return The {@link EventBus} that handled the event and the subscriber.
	 *         Useful for broadcasting a a new event based on the error.
	 */
	public TaskEventBus getEventBus() {
		return eventBus;
	}

	/**
	 * @return The event object that caused the subscriber to throw.
	 */
	public Object getEvent() {
		return event;
	}

	/**
	 * @return The object context that the subscriber was called on.
	 */
	public Object getSubscriber() {
		return subscriber;
	}

	/**
	 * @return The subscribed method that threw the exception.
	 */
	public Method getSubscriberMethod() {
		return subscriberMethod;
	}
}
