package com.hechuan.event.notice.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.hechuan.event.notice.driver.TaskEventBus;

/**
 * 事件监听器
 * 
 * @author hechuan
 *
 * @created 2017年6月8日
 *
 * @version 1.0.0
 */
public class TaskListener implements InitializingBean {
	
	/** 日志 */
	private static final Logger logger = LoggerFactory.getLogger(TaskListener.class);
	
	
	@Autowired
	private TaskEventBus taskEventBus;

	/**
	 * Bean初始化完全后，将自己注册到事件总线中
	 */
	public void afterPropertiesSet() throws Exception {
		logger.info("事件监听注册......begin");
		
		taskEventBus.register(this);
		
		logger.info("事件监听注册......end");
	}
	
}
