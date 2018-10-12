package com.cai.game.mj.universal;

import com.cai.common.domain.GangCardResult;

public abstract class AbstractHandler_Universal<T extends AbstractMahjongTable_Universal> {
	/**
	 * 当前处理器的当前玩家
	 */
	public int currentSeatIndex;

	/**
	 * 当前处理器操作的牌数据
	 */
	public int cardDataHandled;

	/**
	 * 发牌或者吃碰的时候，分析处理的杠牌数据信息
	 */
	public GangCardResult gangCardResult;

	/**
	 * 是吃牌还是碰牌
	 */
	public int chiPengType;

	/**
	 * 杠牌类型，是明杠（直杠、接杠）、暗杠、还是回头杠（碰杠、弯杠）
	 */
	public int gangType;

	/**
	 * 发牌的类型，是开杠发牌，还是正常发牌
	 */
	public int dispatchCardType;

	/**
	 * 当前动作，吃、碰、杠
	 */
	public int currentAction;

	/**
	 * 出牌的类型，是吃碰过后出牌、杠牌并发牌之后出牌，还是正常发牌之后出牌
	 */
	public int outCardType;

	/**
	 * 吃、碰、杠、胡的提供者，也就是玩家的组合牌是谁打出来，吃了、碰了、杠了、胡了的
	 */
	public int providerSeatIndex;

	/**
	 * 切换处理器的时候，需要执行的业务逻辑
	 * 
	 * @param table
	 */
	public abstract void exe(T table);

	/**
	 * 牌桌处于某个处理器的时候，玩家断线重连上来之后的业务逻辑
	 * 
	 * @param table
	 * @param seatIndex
	 * @return
	 */
	public abstract boolean handlePlayerBeInRoom(T table, int seatIndex);

	/**
	 * 牌桌处于某个处理器的时候，客户端发送操作信息到服务端，执行相应的业务逻辑
	 * 
	 * @param table
	 * @param seatIndex
	 * @param operateCode
	 * @param operateCard
	 * @return
	 */
	public abstract boolean handleOperateCard(T table, int seatIndex, int operateCode, int operateCard);

	/**
	 * 牌桌处于吃碰处理器或发牌处理器的时候，客户端发送出牌消息到服务端，执行相应的业务逻辑。 吃碰处理器和发牌处理器必须重写这个方法。
	 * 
	 * @param table
	 * @param seatIndex
	 * @param card
	 * @return
	 */
	public boolean handlePlayerOutCard(T table, int seatIndex, int card) {
		return true;
	}
}
