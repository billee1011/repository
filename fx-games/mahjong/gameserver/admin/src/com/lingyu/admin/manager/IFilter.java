package com.lingyu.admin.manager;

public interface IFilter<T> {
	public boolean check(T t);
}
