package com.hechuan.event.notice.driver;

import com.google.common.collect.Queues;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * 事件总线分发器
 * 
 * @author hechuan
 *
 * @created 2017年6月8日
 *
 * @version 1.0.0
 */
public abstract class Dispatcher {
	
	static Dispatcher perThreadDispatchQueue() {
		return new PerThreadQueuedDispatcher();
	}

	
	static Dispatcher legacyAsync() {
		return new LegacyAsyncDispatcher();
	}

	static Dispatcher immediate() {
		return ImmediateDispatcher.INSTANCE;
	}

	/**
	 * Dispatches the given {@code event} to the given {@code subscribers}.
	 */
	abstract void dispatch(Object event, Iterator<Subscriber> subscribers);

	/**
	 * Implementation of a {@link #perThreadDispatchQueue()} dispatcher.
	 */
	private static final class PerThreadQueuedDispatcher extends Dispatcher {

		// This dispatcher matches the original dispatch behavior of EventBus.

		/**
		 * Per-thread queue of events to dispatch.
		 */
		private final ThreadLocal<Queue<Event>> queue = new ThreadLocal<Queue<Event>>() {
			@Override
			protected Queue<Event> initialValue() {
				return Queues.newArrayDeque();
			}
		};

		/**
		 * Per-thread dispatch state, used to avoid reentrant event dispatching.
		 */
		private final ThreadLocal<Boolean> dispatching = new ThreadLocal<Boolean>() {
			@Override
			protected Boolean initialValue() {
				return false;
			}
		};

		@Override
		void dispatch(Object event, Iterator<Subscriber> subscribers) {
			checkNotNull(event);
			checkNotNull(subscribers);
			Queue<Event> queueForThread = queue.get();
			queueForThread.offer(new Event(event, subscribers));

			if (!dispatching.get()) {
				dispatching.set(true);
				try {
					Event nextEvent;
					while ((nextEvent = queueForThread.poll()) != null) {
						while (nextEvent.subscribers.hasNext()) {
							nextEvent.subscribers.next().dispatchEvent(nextEvent.event);
						}
					}
				} finally {
					dispatching.remove();
					queue.remove();
				}
			}
		}

		private static final class Event {
			private final Object event;
			private final Iterator<Subscriber> subscribers;

			private Event(Object event, Iterator<Subscriber> subscribers) {
				this.event = event;
				this.subscribers = subscribers;
			}
		}
	}

	/**
	 * Implementation of a {@link #legacyAsync()} dispatcher.
	 */
	private static final class LegacyAsyncDispatcher extends Dispatcher {


		/**
		 * Global event queue.
		 */
		private final ConcurrentLinkedQueue<EventWithSubscriber> queue = Queues.newConcurrentLinkedQueue();

		@Override
		void dispatch(Object event, Iterator<Subscriber> subscribers) {
			checkNotNull(event);
			while (subscribers.hasNext()) {
				queue.add(new EventWithSubscriber(event, subscribers.next()));
			}

			EventWithSubscriber e;
			while ((e = queue.poll()) != null) {
				e.subscriber.dispatchEvent(e.event);
			}
		}

		private static final class EventWithSubscriber {
			private final Object event;
			private final Subscriber subscriber;

			private EventWithSubscriber(Object event, Subscriber subscriber) {
				this.event = event;
				this.subscriber = subscriber;
			}
		}
	}

	/**
	 * Implementation of {@link #immediate()}.
	 */
	private static final class ImmediateDispatcher extends Dispatcher {
		private static final ImmediateDispatcher INSTANCE = new ImmediateDispatcher();

		@Override
		void dispatch(Object event, Iterator<Subscriber> subscribers) {
			checkNotNull(event);
			while (subscribers.hasNext()) {
				subscribers.next().dispatchEvent(event);
			}
		}
	}
}
