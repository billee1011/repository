package com.lingyu.game;

import com.lingyu.common.io.Session;

public class GameContext {

	private Session session;

	public GameContext(Session session) {
		this.session = session;
	}

	public Session getSession() {
		return session;
	}
}
