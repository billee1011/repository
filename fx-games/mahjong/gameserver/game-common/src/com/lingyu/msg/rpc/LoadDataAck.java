package com.lingyu.msg.rpc;

import java.util.List;

public class LoadDataAck<T> {

	private List<T> result;

	public List<T> getResult() {
		return result;
	}

	public void setResult(List<T> result) {
		this.result = result;
	}
}
