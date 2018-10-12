package com.lingyu.common.job;

import java.util.concurrent.ScheduledFuture;

public class Schedule {
	private long id;
	private int type;
	private ScheduledFuture<?> future;

	public Schedule() {

	}

	public Schedule(int type, long id, ScheduledFuture<?> future) {
		this.id = id;
		this.type = type;
		this.future = future;
	}

	public String getKey() {
		return type + ":" + id;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public ScheduledFuture<?> getFuture() {
		return future;
	}

	public void setFuture(ScheduledFuture<?> future) {
		this.future = future;
	}

}
