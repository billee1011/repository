package com.lingyu.common.chargecode;


public class _37ChargeCode {
//	1	成功
//	-1	失败
//	-2	用户不存在
//	-3	IP限制
//	-4	md5校验错误 
//	-5	订单号已存在
//	-6	time时间已过期  时间差在 前后3分钟内
//	-7	游戏服务器繁忙
//	-8	参数错误
	public static final _37ChargeCode Success = new _37ChargeCode(1, "成功");
	public static final _37ChargeCode Failed = new _37ChargeCode(-1, "失败");
	public static final _37ChargeCode UserNotExists = new _37ChargeCode(-2, "用户不存在");
	public static final _37ChargeCode IpNotAllowed = new _37ChargeCode(-3, "IP限制");
	public static final _37ChargeCode SigError = new _37ChargeCode(-4, "md5校验错误 ");
	public static final _37ChargeCode OrderDuplicated = new _37ChargeCode(-5, "订单号已存在 ");
	public static final _37ChargeCode TimeExpired = new _37ChargeCode(-6, "time时间已过期");
	public static final _37ChargeCode ServerBusy = new _37ChargeCode(-7, "游戏服务器繁忙");
	public static final _37ChargeCode IllegalParams = new _37ChargeCode(-8, "参数错误");
	private final int code;
	private final String msg;
	public _37ChargeCode(int code, String msg) {
		this.code = code;
		this.msg = msg;
	}
	public int getCode() {
		return code;
	}
	public String getMsg() {
		return msg;
	}
}
