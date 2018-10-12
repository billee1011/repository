package com.lingyu.common.message;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class GameCommand implements Comparable<GameCommand> {
	private byte group;
	private String module;
	private int value;
	private boolean relay;// 是否可以转发到跨服服务器
	private boolean valid = true;
	private boolean print=true;
	private final AtomicLong number = new AtomicLong(0);

	public GameCommand(byte group, String module, int value, boolean relay,boolean print) {
		this.group = group;
		this.module = module;
		this.value = value;
		this.relay = relay;
		this.print=print;
	}

	public void increase() {
		number.incrementAndGet();
	}

	public long getNumber() {
		return number.get();
	}

	public boolean isPrint() {
		return print;
	}

	public void setPrint(boolean print) {
		this.print = print;
	}

	public byte getGroup() {
		return group;
	}

	public void setGroup(byte group) {
		this.group = group;
	}

	public String getModule() {
		return module;
	}

	public void setModule(String module) {
		this.module = module;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	public boolean isRelay() {
		return relay;
	}

	public void setRelay(boolean relay) {
		this.relay = relay;
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	@Override
	public int compareTo(GameCommand o) {
		return (int) (o.getNumber()-this.getNumber());
	}

	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

}
