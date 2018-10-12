package com.lingyu.admin.vo;

import com.lingyu.admin.core.ErrorCode;

/**
 * 登录前检查结果VO.
 * 
 * @author 小流氓<176543888@qq.com>
 */
public class LoginCheckResultVo {
	// ErrorCode值
	private String errcode = ErrorCode.EC_OK;
	// 连续登录错误的次数
	private int loginFailed = 0;
	// 验证码
	private String verifycode = null;

	public String getErrcode() {
		return errcode;
	}

	public void setErrcode(String errcode) {
		this.errcode = errcode;
	}

	public int getLoginFailed() {
		return loginFailed;
	}

	public void setLoginFailed(int loginFailed) {
		this.loginFailed = loginFailed;
	}

	public String getVerifycode() {
		return verifycode;
	}

	public void setVerifycode(String verifycode) {
		this.verifycode = verifycode;
	}
}