package com.cai.core;

import protobuf.clazz.Protocol.Response;

public class ResponseWrapper {
	
	int responseType;

	Response response;

	long startTime;

	public ResponseWrapper(int _responseType, Response _response) {
		this.responseType = _responseType;
		this.response = _response;
		startTime = System.currentTimeMillis();
	}

	public Response getResponse() {
		return response;
	}

	public void setResponse(Response response) {
		this.response = response;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	

	public int getResponseType() {
		return responseType;
	}

	public void setResponseType(int responseType) {
		this.responseType = responseType;
	}

	public long getStartTime() {
		return startTime;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Type=" + responseType);
		sb.append(" Content=" + response);
		return sb.toString();
	}

	public void clean() {
		this.responseType = 0;
		this.response = null;
	}
}
