package com.qding.callable.spring.util;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.web.context.WebApplicationContext;

public class ApplicationContextUtil implements ApplicationListener<ContextRefreshedEvent>{

	private static WebApplicationContext applicationContext;

	public static <T> T getBeansOfType(Class<T> clazz) {
		return (T) applicationContext.getBeansOfType(clazz).values().iterator().next();
	}
	
	public static WebApplicationContext getApplicationContext() {
		return applicationContext;
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		ApplicationContextUtil.applicationContext = (WebApplicationContext) event.getApplicationContext();
	}
}
