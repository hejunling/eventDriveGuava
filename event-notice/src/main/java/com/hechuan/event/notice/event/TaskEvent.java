package com.hechuan.event.notice.event;

import com.google.common.base.MoreObjects;

/**
 * taskEvent
 * 
 * @author hechuan
 *
 * @created 2017年6月8日
 *
 * @version 1.0.0
 */
public class TaskEvent {

	public TaskEvent() {

	}

	public TaskEvent(String taskId) {
		this.taskId = taskId;
	}

	private String taskId;

	public String getTaskId() {
		return taskId;
	}

	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).omitNullValues().add("taskId", taskId).toString();
	}

}
