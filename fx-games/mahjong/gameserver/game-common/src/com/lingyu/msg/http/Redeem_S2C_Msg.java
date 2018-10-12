package com.lingyu.msg.http;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

public class Redeem_S2C_Msg extends HttpMsg implements ISerialaIdable{
	private int retCode;
	private List<String> messages;
	private boolean hasCheckPrivilege;
	
	private int serialId;
	
	public int getRetCode() {
		return retCode;
	}
	public void setRetCode(int retCode) {
		this.retCode = retCode;
	}
	public List<String> getMessages() {
		return messages;
	}
	public void setMessages(List<String> messages) {
		this.messages = messages;
	}
	
	public boolean isHasCheckPrivilege() {
		return hasCheckPrivilege;
	}
	public void setHasCheckPrivilege(boolean hasCheckPrivilege) {
		this.hasCheckPrivilege = hasCheckPrivilege;
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		if(CollectionUtils.isNotEmpty(messages)){
			for(String str : messages){
				sb.append(str).append("<br/>");
			}
		}else{
			sb.append(retCode);
		}
		return sb.toString();
	}
	
	public int getSerialId() {
		return serialId;
	}

	public void setSerialId(int serialId) {
		this.serialId = serialId;
	}
}
