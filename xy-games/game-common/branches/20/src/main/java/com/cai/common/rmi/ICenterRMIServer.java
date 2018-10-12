package com.cai.common.rmi;

import java.util.List;

import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.EMoneyOperateType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountRecommendModel;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.AddMoneyResultModel;
import com.cai.common.domain.LogicStatusModel;
import com.cai.common.domain.ProxyStatusModel;
import com.cai.common.domain.RoomRedisModel;
import com.cai.common.domain.SysParamModel;

import javolution.util.FastMap;
import protobuf.redis.ProtoRedis.RsAccountModelResponse;
import protobuf.redis.ProtoRedis.RsRMIResultResponse;
import protobuf.redis.ProtoRedis.RsSystemStopReadyResultResponse;
import protobuf.redis.ProtoRedis.RsSystemStopReadyStatusResponse;

public interface ICenterRMIServer {
	
	public void sayHello();
	
	
	/**
	 * 不同游戏房间号生成
	 * @param game_id
	 * @return
	 */
	public int randomRoomId(int game_id);
	
	/**
	 * 获取账号信息
	 * @param account_name
	 * @return
	 */
	public Account getAccount(String account_name);
	
	/**
	 * 获取账号信息
	 * @param account_id
	 * @return
	 */
	public Account getAccount(long account_id);
	
	
	/**
	 * 更新账号信息
	 * @param account
	 * @return
	 */
	public Account updateAccount(Account account);
	
	
	/**
	 * 获取参数缓存
	 * @return
	 */
	public FastMap<Integer,FastMap<Integer,SysParamModel>> getSysParamModelDictionary();
	
	
	/**
	 * 重加载参数缓存
	 * @return
	 */
	public boolean reLoadSysParamModelDictionary();
	
	/**
	 * 重加载系统公告字典
	 * @return
	 */
	public boolean reLoadSysNoticeModelDictionary();
	
	
	/**
	 * 重加载连续登录配置
	 * @return
	 */
	public boolean reLoadContinueLoginDictionary();
	
	/**
	 * 重加载系统活动
	 * @return
	 */
	public boolean reLoadActivityDictionary();
	
	/**
	 * 重加载游戏玩玩法说明字典
	 */
	public boolean reLoadGameDescDictionary();
	
	/**
	 * 重新加载服务器列表缓存
	 * @return
	 */
	public boolean reLoadSysParamDict();
	
	/**
	 * 重新加载主界面公告缓存
	 * @return
	 */
	public boolean reLoadMainUiNoticeDictionary();
	
	/**
	 * 重新加载登录公告
	 * @return
	 */
	public boolean reLoadLoginNoticeDictionary();
	
	
	/**
	 * 获取redis上的所有房间
	 * @return
	 */
	public List<RoomRedisModel> getAllRoomRedisModelList();
	
	/**
	 * 删除指定房间
	 * @param room_id
	 */
	public void delRoomById(int room_id);
	
	
	/**
	 * 查询指定房间
	 * @param room_id
	 */
	public RoomRedisModel getRoomById(int room_id);
	
	/**
	 * 获取账号信息，如果没有自动创建
	 * @param pt_flag
	 * @param pt_name
	 * @param ip
	 * @param last_client_flag 设备标识
	 * @param client_version 客户端版本
	 * @return
	 */
	public Account getAndCreateAccount(String pt_flag,String account_name,String ip,String last_client_flag,String client_version);
	
	
	/**
	 * 所有代理服的状态
	 * @return
	 */
	public List<ProxyStatusModel> getProxyStatusList();
	
	
	/**
	 * 所有逻辑服的状态
	 * @return
	 */
	public List<LogicStatusModel> getLogicStatusList();
	
	
	/**
	 * 增减玩家房卡
	 * @param account_id
	 * @param gold
	 * @param isExceed 扣卡是否可以超过原来的数量
	 * @param desc
	 * @return
	 */
	public AddGoldResultModel addAccountGold(long account_id,int gold,boolean isExceed ,String desc,EGoldOperateType eGoldOperateType);
	
	/**
	 * 分配逻辑服
	 * @return
	 */
	public int allotLogicId(int game_id);
	
	/**
	 * 压力测试
	 * @param type
	 * @param num
	 */
	public void myTest(int type,int num);
	
	/**
	 * 实时发送游戏公告
	 * @param game_id
	 * @param content
	 * @param pay_type 
	 */
	public void sendGameNotice(int game_type,String content,int play_type);
	
	/**
	 * 提供给oss修改数据的,只处理已做的
	 */
	public boolean ossModifyAccountModel(RsAccountModelResponse rsAccountModelResponse);
	
	/**
	 * 清除账号缓存
	 * @param account_id
	 * @return
	 */
	public boolean clearAccountCache(long account_id);
	
	/**
	 * 重新加载商品列表
	 */
	public void reloadVailShopList();
	
	/**
	 * 踢下线
	 * @param account_id
	 * @return
	 */
	public boolean offlineAccount(long account_id);
	


	/**
	 * 当前系统关闭准备状态
	 * @return
	 */
	public RsSystemStopReadyStatusResponse systemStopReadyStatus();
	
	/**
	 * 系统关闭准备
	 */
	public RsSystemStopReadyResultResponse systemStopReady(int minute);
	
	/**
	 * 取消系统关闭
	 * @return
	 */
	public RsSystemStopReadyResultResponse systemStopCancel();
	
	/**
	 * 刷新微信缓存
	 * @param account_id
	 * @return
	 */
	public RsRMIResultResponse flushWxCache(long account_id);
	
	
	public String getGameOrderID();
	
	/**
	 * 代理服验证通过 --这里只负责通知中心服 一次  
	 * @param gameOrderID  游戏订单ID
	 */
	public void payCenterCall(String gameOrderID);
	
	
	/**
	 * 订单修复
	 * @param gameOrderID  游戏订单ID
	 */
	public String orderRepair(String gameOrderID);
	
	
	/**
	 * 每日统计修复
	 * @param beforeDay  修复指定日期的数据
	 */
	public void everyDayRepair(int beforeDay);


	/**
	 * 重新加载金币商品列表
	 */
	public void reloadVailMoneyShopList();
	
	
	/**
	 * 增减玩家铜钱
	 * @param account_id
	 * @param gold
	 * @param isExceed 扣卡是否可以超过原来的数量
	 * @param desc
	 * @return
	 */
	public AddMoneyResultModel addAccountMoney(long account_id,int money,boolean isExceed ,String desc,EMoneyOperateType eMoneyOperateType);
	
	
	/**
	 * 后台测试牌型
	 * @param cards  
	 */
	public String testCard(String cards);
	
	
	/**
	 * 添加历史小牌局次数
	 * @param num
	 * @return
	 */
	public boolean addHistorySamllBrandTimes(long account_id,int num);
	
	
	/**
	 * 添加历史大牌局次数
	 * @param num
	 * @return
	 */
	public boolean addHistoryBigBrandTimes(long account_id,int num);
	
	
	//public boolean addHistoryBrandSuccess(long account_id,int num);
	
	/**
	 * 新增邀请记录
	 * @param accountRecommendModel
	 */
	public boolean addAccountRecommendModel(AccountRecommendModel accountRecommendModel);
	
	
	
	
	
}


