package com.cai.constant;

/**
 * 
 * 
 * @see ClubJoinResultRsp
 * @see S2CCmd.CLUB_JOIN_RESULT
 * 
 * @author wu_hc date: 2017年11月28日 下午3:11:56 <br/>
 */
public interface ClubJoinCode {

	// 同意加入
	int AGREE = 1;

	// 拒绝加入
	int REJECT = 2;

	// 踢
	int KICK = 3;
	
	/**
	 * 同意退出
	 */
	int AGREE_QUIT = 4;
}
