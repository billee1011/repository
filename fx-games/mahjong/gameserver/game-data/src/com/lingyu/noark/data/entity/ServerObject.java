package com.lingyu.noark.data.entity;

import com.lingyu.noark.data.annotation.Column;
import com.lingyu.noark.data.annotation.Id;


public class ServerObject {
	@Id
	@Column(name = "id")
	protected long id;

	public final long getId() {
		return id;
	}

	public final void setId(long id) {
		this.id = id;
	}
}
