package com.lingyu.admin.vo;

import java.util.ArrayList;
import java.util.List;

public class PlayerVos {
	private String errorCode = "";
	private List<PlayerVo> playerVos = new ArrayList<PlayerVo>();
	
	public String getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}
	public List<PlayerVo> getPlayerVos() {
		return playerVos;
	}
	public void setPlayerVos(List<PlayerVo> playerVos) {
		this.playerVos = playerVos;
	}
	
	public void addPlayerVo(PlayerVo playerVo){
		playerVos.add(playerVo);
	}
}
