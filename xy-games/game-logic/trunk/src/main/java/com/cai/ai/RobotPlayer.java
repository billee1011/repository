package com.cai.ai;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import com.cai.coin.DataWrap;
import com.cai.coin.excite.condition.ExciteConditionGroup;
import com.cai.common.base.BaseTask;
import com.cai.common.define.ETriggerType;
import com.cai.common.domain.CardCategoryModel;
import com.cai.common.domain.CoinCornucopiaModel;
import com.cai.common.domain.CoinExciteModel;
import com.cai.common.domain.Player;
import com.cai.common.domain.Room;
import com.cai.dictionary.CardCategoryDict;
import com.cai.game.AbstractRoom;
import com.cai.service.AiService;
import com.cai.util.ICardCategoryBehaviour;

import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.RoomResponse;

public class RobotPlayer extends Player implements ICardCategoryBehaviour {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public volatile transient Future<?> future;
	public volatile transient BaseTask task;
	private volatile transient Response curResponse;
	//超时时间
	private transient int outTimeCount;

	//是否参与聚宝盆玩法
	private boolean cornucopia = false;
	private int recycleCoin;

	//条件
	private final transient ExciteConditionGroup conditionGroup = new ExciteConditionGroup(this);

	public void operationAi() {
		if (!isOp()) {
			return;
		}

		if (getMyRoom().isPauseGame) {
			return;
		}
		cancel(false);
		if (task != null) {
			AiService.getInstance().execute(getAccount_id(), task);
		}
	}

	public void useAi(Response response) {
		if (!isOp()) {
			return;
		}

		if (response.getResponseType() == ResponseType.ROOM) {
			RoomResponse rsp = AiService.getInstance().get(response);
			sendMsg(response, rsp);
		}
	}

	@SuppressWarnings("unchecked")
	private <T extends AbstractRoom> void sendMsg(Response response, RoomResponse rsp) {
		if (getGameAi() != null && rsp != null) {
			AbstractAi<T> handler = (AbstractAi<T>) getGameAi().get(rsp.getType());
			if (handler != null) {
				if (getMyRoom().isPauseGame) {
					this.curResponse = response;
					return;
				}
				AiHandleTask<T> task = new AiHandleTask<T>(handler, (T) getMyRoom(), this, rsp);
				AiService.getInstance().execute(getAccount_id(), task);
				this.curResponse = response;
			}
		}
	}

	@Override
	public void pauseAi() {
		cancel();
	}

	@Override
	public void continueAi() {
		if (curResponse != null) {
			useAi(curResponse);
		}
	}

	public void cancel() {
		cancel(true);
	}

	private void cancel(boolean isRestTask) {
		if (isRestTask) {
			task = null;
		}
		if (future != null) {
			try {
				future.cancel(true);
				future = null;
				setPlay_card_time(0);
			} catch (Exception e) {
			}
		}
	}

	//玩家超时多次或者其他情况要自动出牌
	public boolean isAuto() {
		return isRobot();
	}

	public int getOutTimeCount() {
		return outTimeCount;
	}

	public void setOutTimeCount(int outTimeCount) {
		this.outTimeCount = outTimeCount;
	}

	/**
	 * 当前正在进行中的游戏房间
	 */
	public AbstractRoom getMyRoom() {
		AbstractRoom curRoom = null;
		Room mRoom = getCurRoom();
		if (mRoom != null) {
			curRoom = (AbstractRoom) mRoom;
		}
		return curRoom;
	}

	private Map<Integer, AbstractAi<?>> getGameAi() {
		Map<Integer, AbstractAi<?>> gameAi = null;
		AbstractRoom curRoom = getMyRoom();
		if (curRoom != null) {
			int gameTypeIndex = curRoom.getGameTypeIndex();
			int roomType = curRoom.getCreate_type();
			gameAi = AiService.getInstance().getAiByGameId(roomType, gameTypeIndex);
		}
		return gameAi;
	}

	/**
	 * 是否可进行机器人操作
	 */
	private boolean isOp() {
		if (getMyRoom() == null) {
			return false;
		}

		if (!getMyRoom().isEnableRobot()) {
			return false;
		}

		if (getGameAi() == null) {
			return false;
		}
		return true;
	}

	@Override
	public void readyGame(int seat_index) {
		if (isOp()) {
			getMyRoom().handler_player_auto_ready(seat_index);
		}
	}

	/**
	 * 初始化特殊玩法加成相关
	 *
	 * @param models
	 */
	public void initConditionIfHas(DataWrap.Type type, List<CoinExciteModel> models, Consumer<DataWrap> callback) {

		if (models.isEmpty()) {
			return;
		}

		for (CoinExciteModel model : models) {
			CardCategoryModel categoryModel = CardCategoryDict.getInstance().getModel(model.getCategoryId());
			if (null != categoryModel) {
				conditionGroup.addCondition(type, model, categoryModel);
				conditionGroup.setCallBack(callback);
			}
		}
	}

	/**
	 * @return
	 */
	public final ExciteConditionGroup getConditionGroup() {
		return conditionGroup;
	}

	/**
	 * 产生的倍数
	 *
	 * @return
	 */
	public int getExciteMultiple() {
		return Math.max(1, conditionGroup.getOverMultiple(DataWrap.Type.EXCITE));
	}

	@Override
	public void triggerEvent(ETriggerType triggerType, long cardTypeValue, int value) {
		conditionGroup.triggerEvent(triggerType, cardTypeValue, value);
		logger.info("coinTable triggerEvent -> player[{},{}] triggerType[{}], cardTypeValue[{}],value[{}]", getAccount_id(), getNick_name(),
				triggerType, cardTypeValue, value);
	}

	@Override
	public void triggerCardEvent(ETriggerType triggerType, int[] cardArray) {
		conditionGroup.triggerCardEvent(triggerType, cardArray);
	}

	@Override
	public void triggerEventOver(ETriggerType triggerType) {
		conditionGroup.triggerEventOver(triggerType);
	}

	public boolean isCornucopia() {
		return cornucopia;
	}

	public void setCornucopia(boolean cornucopia) {
		this.cornucopia = cornucopia;
	}

	public int getRecycleCoin() {
		return recycleCoin;
	}

	public void setRecycleCoin(int recycleCoin) {
		this.recycleCoin = recycleCoin;
	}
}
