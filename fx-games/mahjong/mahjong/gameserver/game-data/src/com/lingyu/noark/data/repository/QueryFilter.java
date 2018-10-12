package com.lingyu.noark.data.repository;

public interface QueryFilter<T> {
	/**
	 * 验证指定实体是否满足条件
	 * 
	 * @param t 实体
	 */
	public boolean check(T t);

	/**
	 * 是否停止查询,用于控制查询数量所需
	 */
	public boolean stopped();
}
