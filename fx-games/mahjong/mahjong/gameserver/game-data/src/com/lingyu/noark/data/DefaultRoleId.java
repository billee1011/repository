package com.lingyu.noark.data;

import java.io.Serializable;

/**
 * 默认的角色ID实现类.
 * 
 * @author 小流氓<176543888@qq.com>
 */
public class DefaultRoleId implements Serializable {
	private static final long serialVersionUID = 4886721295480965055L;
	public final static DefaultRoleId instance = new DefaultRoleId();

	private DefaultRoleId() {
	}

	@Override
	public String toString() {
		return "System";
	}
}
