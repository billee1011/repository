package com.lingyu.noark.data.accessor;

public class Sort {
	private final Direction direction;
	private final String[] fields;

	public Sort(Direction direction, String... fields) {
		this.direction = direction;
		this.fields = fields;
	}

	public final String[] getFields() {
		return fields;
	}

	public final Direction getDirection() {
		return direction;
	}

	public enum Direction {
		DESC, ASC
	}
}