package com.hechuan.event.notice.driver;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * 广播事件实体
 * 
 * @author hechuan
 *
 * @created 2017年6月8日
 *
 * @version 1.0.0
 */
public class PostEvent {

	public static final String DEFAULT = "default";

	private String taskId;

	private Object event;

	public static PostEvent create() {
		return new PostEvent();
	}

	public PostEvent() {
	}

	public String getTaskId() {
		return taskId;
	}

	public PostEvent setTaskId(String taskId) {
		this.taskId = checkNotNull(taskId);
		return this;
	}

	public Object getEvent() {
		return event;
	}

	public PostEvent setEvent(Object event) {
		this.event = checkNotNull(event);
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(taskId, event);
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof PostEvent) {
			PostEvent ident = (PostEvent) o;
			return taskId.equals(ident.taskId) && Objects.equal(event, ident.event);
		}
		return false;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper("PostEvent").add("taskId", taskId).add("event", event).toString();
	}

}
