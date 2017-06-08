package com.hechuan.event.notice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Preconditions;
import com.hechuan.event.notice.driver.PostEvent;
import com.hechuan.event.notice.driver.TaskEventBus;
import com.hechuan.event.notice.event.TaskEvent;

/**
 * 事件controller
 * 
 * @author hechuan
 *
 * @created 2017年6月8日
 *
 * @version 1.0.0
 */
@RequestMapping("/event")
@RestController
public class EventController {

	private static final Logger logger = LoggerFactory.getLogger(EventController.class);

	private static final String SUCCESS = "SUCCESS";

	@Autowired
	private TaskEventBus taskEventBus;

	/**
	 * 执行任务方法
	 * 
	 * @param taskId
	 *            任务ID
	 * @return execute SUCCESS|FAILED
	 */
	@RequestMapping(value = "/do", method = RequestMethod.POST)
	public String doTask(@RequestParam String taskId) {
		logger.info("EventController.doTask execte....begin...");
		Preconditions.checkNotNull(taskId);

		PostEvent postEvent = PostEvent.create().setTaskId(taskId).setEvent(new TaskEvent(taskId));

		taskEventBus.post(postEvent);

		logger.info("EventController.doTask execte....end...");

		return SUCCESS;
	}
}
