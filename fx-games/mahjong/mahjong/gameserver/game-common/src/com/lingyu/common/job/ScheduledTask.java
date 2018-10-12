package com.lingyu.common.job;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.scheduling.support.MethodInvokingRunnable;

/**
 * @author Allen Jiang
 * @since 2014-01-23
 * @version 1.0
 **/
public class ScheduledTask extends MethodInvokingRunnable implements FactoryBean<Runnable> {

	@Override
	public void afterPropertiesSet() throws ClassNotFoundException, NoSuchMethodException, IllegalArgumentException {
		super.afterPropertiesSet();

	}

	public Runnable getObject() throws Exception {
		return this;
	}

	public Class<MethodInvokingRunnable> getObjectType() {
		return MethodInvokingRunnable.class;
	}

	public boolean isSingleton() {
		return false;
	}
}
