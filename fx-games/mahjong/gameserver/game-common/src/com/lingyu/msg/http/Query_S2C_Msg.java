package com.lingyu.msg.http;

import java.util.ArrayList;
import java.util.List;

public class Query_S2C_Msg extends HttpMsg{
	private int retCode;
	private List<QueryDTO> list = new ArrayList<>();
	private long interval;

	public int getRetCode() {
		return retCode;
	}

	public void setRetCode(int retCode) {
		this.retCode = retCode;
	}

	public List<QueryDTO> getList() {
		return list;
	}

	public void setList(List<QueryDTO> list) {
		this.list = list;
	}

	public long getInterval() {
		return interval;
	}

	public void setInterval(long interval) {
		this.interval = interval;
	}

	public void add(QueryDTO dto) {
		list.add(dto);
	}
}
