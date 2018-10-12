package com.lingyu.common.io;

import org.apache.commons.lang3.RandomUtils;

public class SessionId {
	private final long id;

	public SessionId(long id) {
		super();
		this.id = id;
	}

	public static SessionId nextId() {
		return new SessionId(RandomUtils.nextLong(0, Long.MAX_VALUE));
	}

	public long getId() {
		return id;
	}
}
