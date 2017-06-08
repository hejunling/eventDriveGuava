package com.hechuan.event.notice.driver;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.UncheckedExecutionException;

/**
 * 事件总线监听注册器
 * 
 * @author hechuan
 *
 * @created 2017年6月8日
 *
 * @version 1.0.0
 */
public class SubscriberRegistry {

	private final ConcurrentMap<SubscriberIdentifier, CopyOnWriteArraySet<Subscriber>> subscribers = Maps
			.newConcurrentMap();

	/**
	 * The event bus this registry belongs to.
	 */
	private final TaskEventBus bus;

	SubscriberRegistry(TaskEventBus bus) {
		this.bus = checkNotNull(bus);
	}

	/**
	 * Registers all subscriber methods on the given listener object.
	 */
	void register(Object listener) {
		Multimap<SubscriberIdentifier, Subscriber> listenerMethods = findAllSubscribers(listener);

		for (Map.Entry<SubscriberIdentifier, Collection<Subscriber>> entry : listenerMethods.asMap().entrySet()) {
			SubscriberIdentifier identifier = entry.getKey();
			Collection<Subscriber> eventMethodsInListener = entry.getValue();

			CopyOnWriteArraySet<Subscriber> eventSubscribers = subscribers.get(identifier);

			if (eventSubscribers == null) {
				CopyOnWriteArraySet<Subscriber> newSet = new CopyOnWriteArraySet<Subscriber>();
				eventSubscribers = MoreObjects.firstNonNull(subscribers.putIfAbsent(identifier, newSet), newSet);
			}

			eventSubscribers.addAll(eventMethodsInListener);
		}
	}

	/**
	 * Unregisters all subscribers on the given listener object.
	 */
	void unregister(Object listener) {
		Multimap<SubscriberIdentifier, Subscriber> listenerMethods = findAllSubscribers(listener);

		for (Map.Entry<SubscriberIdentifier, Collection<Subscriber>> entry : listenerMethods.asMap().entrySet()) {
			SubscriberIdentifier identifier = entry.getKey();
			Collection<Subscriber> listenerMethodsForType = entry.getValue();

			CopyOnWriteArraySet<Subscriber> currentSubscribers = subscribers.get(identifier);
			if (currentSubscribers == null || !currentSubscribers.removeAll(listenerMethodsForType)) {
				throw new IllegalArgumentException(
						"missing event subscriber for an annotated method. Is " + listener + " registered?");
			}
		}
	}

	@VisibleForTesting
	Set<Subscriber> getSubscribersForTesting(SubscriberIdentifier identifier) {
		return MoreObjects.firstNonNull(subscribers.get(identifier), ImmutableSet.<Subscriber> of());
	}

	Iterator<Subscriber> getSubscribers(PostEvent event) {
		ImmutableSet<SubscriberIdentifier> identifiers = flattenHierarchy(fromPostEvent(event));

		List<Iterator<Subscriber>> subscriberIterators = Lists.newArrayListWithCapacity(identifiers.size());

		for (SubscriberIdentifier identifier : identifiers) {
			CopyOnWriteArraySet<Subscriber> eventSubscribers = subscribers.get(identifier);
			if (eventSubscribers != null) {
				// eager no-copy snapshot
				subscriberIterators.add(eventSubscribers.iterator());
			}
		}

		return Iterators.concat(subscriberIterators.iterator());
	}

	private static final LoadingCache<Class<?>, ImmutableList<Method>> subscriberMethodsCache = CacheBuilder
			.newBuilder().weakKeys().build(new CacheLoader<Class<?>, ImmutableList<Method>>() {
				@Override
				public ImmutableList<Method> load(Class<?> concreteClass) throws Exception {
					return getAnnotatedMethodsNotCached(concreteClass);
				}
			});

	private Multimap<SubscriberIdentifier, Subscriber> findAllSubscribers(Object listener) {
		Multimap<SubscriberIdentifier, Subscriber> methodsInListener = HashMultimap.create();
		Class<?> clazz = listener.getClass();
		for (Method method : getAnnotatedMethods(clazz)) {
			Class<?>[] parameterTypes = method.getParameterTypes();
			Class<?> eventType = parameterTypes[0];
			Subscribe subscribe = method.getAnnotation(Subscribe.class);
			SubscriberIdentifier identifier = new SubscriberIdentifier(subscribe.taskId(), eventType);
			methodsInListener.put(identifier, Subscriber.create(bus, listener, method));
		}
		return methodsInListener;
	}

	private static ImmutableList<Method> getAnnotatedMethods(Class<?> clazz) {
		return subscriberMethodsCache.getUnchecked(clazz);
	}

	private static ImmutableList<Method> getAnnotatedMethodsNotCached(Class<?> clazz) {
		Set<? extends Class<?>> supertypes = TypeToken.of(clazz).getTypes().rawTypes();
		Map<MethodIdentifier, Method> identifiers = Maps.newHashMap();
		for (Class<?> supertype : supertypes) {
			for (Method method : supertype.getDeclaredMethods()) {
				if (method.isAnnotationPresent(Subscribe.class) && !method.isSynthetic()) {
					Class<?>[] parameterTypes = method.getParameterTypes();
					checkArgument(parameterTypes.length == 1,
							"Method %s has @Subscribe annotation but has %s parameters."
									+ "Subscriber methods must have exactly 1 parameter.",
							method, parameterTypes.length);

					MethodIdentifier ident = new MethodIdentifier(method);
					if (!identifiers.containsKey(ident)) {
						identifiers.put(ident, method);
					}
				}
			}
		}
		return ImmutableList.copyOf(identifiers.values());
	}

	/**
	 * Global cache of classes to their flattened hierarchy of supertypes.
	 */
	private static final LoadingCache<SubscriberIdentifier, ImmutableSet<SubscriberIdentifier>> flattenHierarchyCache = CacheBuilder
			.newBuilder().weakKeys().build(new CacheLoader<SubscriberIdentifier, ImmutableSet<SubscriberIdentifier>>() {
				
				@Override
				public ImmutableSet<SubscriberIdentifier> load(final SubscriberIdentifier concreteIdentifier) {

					// 取得当前事件类的所有父类和接口列表
					ImmutableSet<Class<?>> eventTypes = ImmutableSet
							.<Class<?>> copyOf(TypeToken.of(concreteIdentifier.eventType).getTypes().rawTypes());

					// 将类型列表转换成监听身份列表
					Set<SubscriberIdentifier> identifiers = FluentIterable.from(eventTypes)
							.transform(new Function<Class<?>, SubscriberIdentifier>() {
								@Override
								public SubscriberIdentifier apply(Class<?> input) {
									return new SubscriberIdentifier(concreteIdentifier.taskId, input);
								}
							}).toSet();

					return ImmutableSet.copyOf(identifiers);
				}
			});

	@VisibleForTesting
	static ImmutableSet<SubscriberIdentifier> flattenHierarchy(SubscriberIdentifier concreteIdentifier) {
		try {
			return flattenHierarchyCache.getUnchecked(concreteIdentifier);
		} catch (UncheckedExecutionException e) {
			throw Throwables.propagate(e.getCause());
		}
	}

	/**
	 * 将广播参数转换为订阅者标识
	 *
	 * @param event
	 *            广播参数
	 * @return 订阅者标识
	 */
	private SubscriberIdentifier fromPostEvent(PostEvent event) {

		return new SubscriberIdentifier(event.getTaskId(), event.getEvent().getClass());
	}

	private static final class MethodIdentifier {

		private final String name;
		private final List<Class<?>> parameterTypes;

		MethodIdentifier(Method method) {
			this.name = method.getName();
			this.parameterTypes = Arrays.asList(method.getParameterTypes());
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(name, parameterTypes);
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof MethodIdentifier) {
				MethodIdentifier ident = (MethodIdentifier) o;
				return name.equals(ident.name) && parameterTypes.equals(ident.parameterTypes);
			}
			return false;
		}
	}

	private static final class SubscriberIdentifier {

		private final String taskId;
		private final Class<?> eventType;

		public SubscriberIdentifier(String taskId, Class<?> eventType) {
			this.taskId = taskId;
			this.eventType = eventType;
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(taskId, eventType);
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof SubscriberIdentifier) {
				SubscriberIdentifier ident = (SubscriberIdentifier) o;
				return taskId.equals(ident.taskId) && eventType == ident.eventType;
			}
			return false;
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper("SubscriberIdentifier").add("eventType", eventType).add("taskId", taskId)
					.toString();
		}
	}
}
