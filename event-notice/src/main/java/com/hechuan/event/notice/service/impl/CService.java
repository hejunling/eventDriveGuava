package com.hechuan.event.notice.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.hechuan.event.notice.driver.Subscribe;
import com.hechuan.event.notice.event.TaskEvent;
import com.hechuan.event.notice.listener.TaskListener;

/**
 * Cservice
 * @author hechuan
 *
 * @created 2017年6月8日
 *
 * @version 1.0.0
 */
@Component
public class CService extends TaskListener{

	private static final Logger logger = LoggerFactory.getLogger(AService.class);

	@Subscribe(taskId = "3")
	public String execute(TaskEvent event) {
		
		logger.info("event ：{}",event);
		
		return "taskId = 3 exectue";
	}

}
