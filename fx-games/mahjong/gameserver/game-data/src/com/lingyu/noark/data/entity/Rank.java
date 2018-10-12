package com.lingyu.noark.data.entity;

import com.lingyu.noark.data.accessor.redis.cmd.Zadd;

public class Rank extends Zadd {

	public Rank(long roleId, String key, double sorce) {
		super(roleId, key, sorce, String.valueOf(roleId));
	}
}