package com.cai.core;

import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Response;

public class ResponseWrapper {
	
	private int responseType;

	private int respQueueIdx;

	private Response response;

	private long startTime;
	
	private int prox_id;
	private long prox_session_id;
	private int game_index;
	private int game_id;

	public ResponseWrapper(int _responseType, Response _response,Request topRequest) {
		this.responseType = _responseType;
		this.response = _response;
		this.prox_id = topRequest.getProxId();
		this.prox_session_id = topRequest.getProxSeesionId();
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

	public int getRespQueueIdx() {
		return respQueueIdx;
	}

	public void setRespQueueIdx(int respQueueIdx) {
		this.respQueueIdx = respQueueIdx;
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

	public int getProx_id() {
		return prox_id;
	}

	public void setProx_id(int prox_id) {
		this.prox_id = prox_id;
	}

	public long getProx_session_id() {
		return prox_session_id;
	}

	public void setProx_session_id(long prox_session_id) {
		this.prox_session_id = prox_session_id;
	}

	public int getGame_index() {
		return game_index;
	}

	public void setGame_index(int game_index) {
		this.game_index = game_index;
	}

	public int getGame_id() {
		return game_id;
	}

	public void setGame_id(int game_id) {
		this.game_id = game_id;
	}
}
