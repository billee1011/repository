package com.cai.http;

/**
 * Java SDK for OpenAPI V3 - 定义错误码
 *
 * @version 3.0.0
 * @since jdk1.5
 * @author mail: zdl1016@gmail.com qq:33384782 @ History: 3.0.0 | Zhang
 * Dongliang | 2012-03-21 09:43:11 | initialization
 */
public class ErrorCode {

	// 序列化UID
	private static final long serialVersionUID = -1679458253208555786L;

	/**
	 * 成功
	 */
	public final static int SUCCESS = 0;

	/**
	 * 必填参数为空。
	 */
	public final static int PARAMETER_EMPTY = 2001;

	/**
	 * 必填参数无效。
	 */
	public final static int PARAMETER_INVALID = 2002;

	/**
	 * 服务器响应数据无效。
	 */
	public final static int RESPONSE_DATA_INVALID = 2003;

	/**
	 * 生成签名失败。
	 */
	public final static int MAKE_SIGNATURE_ERROR = 2004;

	/**
	 * 时间搓过期
	 */
	public final static int TIME_INVALID = 2005;

	/**
	 * 找不到玩家
	 */
	public final static int ROLE_FIND_FAIL = 4001;

	/**
	 * 找不到商品列表
	 */
	public final static int SHOP_FIND_FAIL = 4002;

	/**
	 * 充值商品无效
	 */
	public final static int SHOP_ERROR_FAIL = 5001;

	/**
	 * 游戏服务器返回充值失败
	 */
	public final static int GAME_ERROR_FAIL = 5002;

	/**
	 * 游戏服务器网络异常
	 */
	public final static int GAME_NET_FAIL = 5003;

	/**
	 * 未定义错误码
	 */
	public final static int UNDIFINED_ERROR_CODE = 10000;

}
