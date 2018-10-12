package com.lingyu.common.id;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lingyu.noark.data.annotation.Column;
import com.lingyu.noark.data.annotation.Id;

public abstract class ServerObject {
	private static final Logger logger = LogManager.getLogger(ServerObject.class);
	@Id
	@Column(name = "id")
	protected long id;

	public void newId() {
		if (id > 0) {
			logger.warn("newId生成似乎有问题，name={},type={},id={}", this.getClass().getSimpleName(), this.getObjectType(), this.getId());
		} else {
			this.id = IdFactory.getInstance().generateId(getObjectType());
		}
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public byte getObjectType() {
		return ServerObjectType.COMMON;
	}

	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
