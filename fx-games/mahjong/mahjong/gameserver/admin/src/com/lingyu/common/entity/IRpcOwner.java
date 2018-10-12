package com.lingyu.common.entity;

public interface IRpcOwner {
	public String getIp();
	public int getPort();
	public boolean isValid();
	public int getFollowerId();
}
