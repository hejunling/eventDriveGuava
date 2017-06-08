package com.hechuan.event.notice.driver;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.AllowConcurrentEvents;

/**
 * 事件总线监听对象容器类，用于放置监听方法的相关属性
 * 
 * @author hechuan
 *
 * @created 2017年6月8日
 *
 * @version 1.0.0
 */
public class Subscriber {

	/**
	 * Creates a {@code Subscriber} for {@code method} on {@code listener}.
	 */
	static Subscriber create(TaskEventBus bus, Object listener, Method method) {
		return isDeclaredThreadSafe(method) ? new Subscriber(bus, listener, method)
				: new SynchronizedSubscriber(bus, listener, method);
	}

	/** The event bus this subscriber belongs to. */
	private TaskEventBus bus;

	/** Object sporting the subscriber method. */
	@VisibleForTesting
	final Object target;

	/** Subscriber method. */
	private final Method method;

	/** Executor to use for dispatching events to this subscriber. */
	private final Executor executor;

	private Subscriber(TaskEventBus bus, Object target, Method method) {
		this.bus = bus;
		this.target = checkNotNull(target);
		this.method = method;
		method.setAccessible(true);

		this.executor = bus.executor();
	}

	/**
	 * Dispatches {@code event} to this subscriber using the proper executor.
	 */
	final void dispatchEvent(final Object event) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					invokeSubscriberMethod(event);
				} catch (InvocationTargetException e) {
					bus.handleSubscriberException(e.getCause(), context(event));
				}
			}
		});
	}

	/**
	 * Invokes the subscriber method. This method can be overridden to make the
	 * invocation synchronized.
	 */
	@VisibleForTesting
	void invokeSubscriberMethod(Object event) throws InvocationTargetException {
		try {
			method.invoke(target, checkNotNull(event));
		} catch (IllegalArgumentException e) {
			throw new Error("Method rejected target/argument: " + event, e);
		} catch (IllegalAccessException e) {
			throw new Error("Method became inaccessible: " + event, e);
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof Error) {
				throw (Error) e.getCause();
			}
			throw e;
		}
	}

	/**
	 * Gets the context for the given event.
	 */
	private SubscriberExceptionContext context(Object event) {
		return new SubscriberExceptionContext(bus, event, target, method);
	}

	@Override
	public final int hashCode() {
		return (31 + method.hashCode()) * 31 + System.identityHashCode(target);
	}

	@Override
	public final boolean equals(Object obj) {
		if (obj instanceof Subscriber) {
			Subscriber that = (Subscriber) obj;
			return target == that.target && method.equals(that.method);
		}
		return false;
	}

	/**
	 * Checks whether {@code method} is thread-safe, as indicated by the
	 * presence of the {@link AllowConcurrentEvents} annotation.
	 */
	private static boolean isDeclaredThreadSafe(Method method) {
		return method.getAnnotation(AllowConcurrentEvents.class) != null;
	}

	/**
	 * Subscriber that synchronizes invocations of a method to ensure that only
	 * one thread may enter the method at a time.
	 */
	@VisibleForTesting
	static final class SynchronizedSubscriber extends Subscriber {

		private SynchronizedSubscriber(TaskEventBus bus, Object target, Method method) {
			super(bus, target, method);
		}

		@Override
		void invokeSubscriberMethod(Object event) throws InvocationTargetException {
			synchronized (this) {
				super.invokeSubscriberMethod(event);
			}
		}
	}

}
