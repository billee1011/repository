package com.cai.http.model;

/**
 * 
 * @author xwy
 * @date 2016年11月21日 -- 上午11:32:35
 *
 */
public class ErrorCode {

	/**
	 * 必填参数无效。
	 */
	public final static String PARAMETER_INVALID_STR = "必填参数无效";

	/**
	 * 必填参数为空。
	 */
	public final static String PARAMETER_EMPTY = "必填参数为空";

	/**
	 * 生成签名失败。
	 */
	public final static String MAKE_SIGNATURE_ERROR = "生成签名失败";

	/**
	 * 时间搓过期
	 */
	public final static String TIME_INVALID = "时间搓过期";

	/**
	 * 找不到玩家
	 */
	public final static String ROLE_FIND_FAIL = "玩家不存在";

	/**
	 * 找不到玩家
	 */
	public final static String VISIT_FAIL = "非推广员无权访问";

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
