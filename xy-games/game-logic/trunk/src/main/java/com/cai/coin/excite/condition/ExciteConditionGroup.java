package com.cai.coin.excite.condition;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import com.cai.ai.RobotPlayer;
import com.cai.coin.DataWrap;
import com.cai.common.define.ECardCategory;
import com.cai.common.define.ETriggerType;
import com.cai.common.domain.CardCategoryModel;
import com.cai.common.domain.CoinExciteModel;
import com.cai.common.util.FilterUtil;
import com.cai.util.ICardCategoryBehaviour;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import protobuf.clazz.Common.CommonLI;
import protobuf.clazz.coin.CoinProtocol;
import protobuf.clazz.coin.CoinProtocol.CardCategoryProto;

/**
 * 刺激玩法/聚宝盆条件管理中心
 *
 * @author wu_hc date: 2018年08月10日 下午3:00:19 <br/>
 */
public final class ExciteConditionGroup implements ICardCategoryBehaviour {

	/**
	 * 条件列表
	 */
	private final EnumMap<DataWrap.Type, Map<Integer, IExciteCondition>> conditionMap = Maps.newEnumMap(DataWrap.Type.class);

	/**
	 * 牌型值，触发次数/余牌数量
	 */
	private final Map<Long, Integer> cardCategoryMap = Maps.newHashMap(); //中途和结束
	private final Map<Long, Integer> startCategoryMap = Maps.newHashMap(); //起手
	/**
	 * 达成条件时返回
	 */
	private Consumer<DataWrap> callBack;

	/**
	 * 起手牌
	 */
	private int[] startCardArray;

	/**
	 * 结束牌
	 */
	private int[] overCardArray;

	/**
	 * 所属的玩家id
	 */
	private RobotPlayer player;

	public ExciteConditionGroup(RobotPlayer player) {
		this.player = player;
	}

	/**
	 * @param triggerType
	 * @return
	 */
	public Optional<CoinProtocol.CardArrayProto> toCardProto(ETriggerType triggerType) {
		if (triggerType == ETriggerType.START) {
			return toCardPB(startCardArray);
		}

		if (triggerType == ETriggerType.OVER) {
			return toCardPB(overCardArray);
		}
		return Optional.empty();
	}

	/**
	 * @param triggerType   　触发时机
	 * @param cardTypeValue 　牌型值
	 * @param value         　数值
	 */
	@Override
	public void triggerEvent(ETriggerType triggerType, long cardTypeValue, int value) {

		if (ETriggerType.START == triggerType) {
			startCategoryMap.compute(cardTypeValue, (k, v) -> null == v ? value : v + value);
			return;
		}

		cardCategoryMap.compute(cardTypeValue, (k, v) -> null == v ? value : v + value);
	}

	/**
	 * 上报起手牌和结束牌
	 *
	 * @param triggerType
	 * @param cardArray
	 */
	@Override
	public void triggerCardEvent(ETriggerType triggerType, int[] cardArray) {
		if (null == cardArray) {
			return;
		}

		if (triggerType == ETriggerType.START) {
			startCardArray = cardArray;
			return;
		}

		if (triggerType == ETriggerType.OVER) {
			overCardArray = cardArray;
		}
	}

	@Override
	public void triggerEventOver(ETriggerType triggerType) {
		if (triggerType == ETriggerType.START || triggerType == ETriggerType.OVER) {
			checkIsConditionFinish(triggerType);
		}
	}

	/**
	 * @param triggerType
	 */
	private void checkIsConditionFinish(ETriggerType triggerType) {

		//回调为空，不需要做任何判断
		if (null == this.callBack) {
			return;
		}

		Map<Long, Integer> suitCategoryMap = ETriggerType.START == triggerType ? startCategoryMap : cardCategoryMap;

		conditionMap.forEach((type, typeConditionMap) -> {

			if (type == DataWrap.Type.CORNUCOPIA && !player.isCornucopia()) {
				return;
			}

			Collection<IExciteCondition> conditions = FilterUtil.filter(typeConditionMap.values(), (cdt) -> cdt.triggerType() == triggerType);

			List<IExciteCondition> cdts = null;

			for (IExciteCondition cdt : conditions) {

				Integer newValue = suitCategoryMap.get(cdt.cardTypeValue());
				if (null == newValue) {
					continue;
				}

				if (cdt.isDone(newValue.intValue())) {
					if (null == cdts) {
						cdts = Lists.newArrayListWithCapacity(6);
					}
					cdts.add(cdt);
				}
			}

			if (null != cdts && !cdts.isEmpty()) {
				DataWrap wrap = new DataWrap();
				wrap.cdts = Collections.unmodifiableList(cdts);
				wrap.type = type;
				wrap.accountId = player.getAccount_id();
				this.callBack.accept(wrap);
			}
		});

	}

	public Consumer<DataWrap> getCallBack() {
		return callBack;
	}

	public void setCallBack(Consumer<DataWrap> callBack) {
		this.callBack = callBack;
	}

	/**
	 * 添加条件
	 *
	 * @param type      {@see DataWrap.Type} 条件类型
	 * @param condition 条件
	 */
	public void addCondition(DataWrap.Type type, IExciteCondition condition) {
		Map<Integer, IExciteCondition> map = conditionMap.computeIfAbsent(type, m -> new HashMap<>());
		map.putIfAbsent(condition.id(), condition);
	}

	/**
	 * @param type          {@see DataWrap.Type} 条件类型
	 * @param model         条件model
	 * @param categoryModel 牌型model
	 */
	public void addCondition(DataWrap.Type type, CoinExciteModel model, final CardCategoryModel categoryModel) {
		Map<Integer, IExciteCondition> map = conditionMap.computeIfAbsent(type, m -> new HashMap<>());

		map.putIfAbsent(model.getId(), ECardCategory.SURPLUS.id() == categoryModel.getId() ?
				new SurplusExciteCondition(model, categoryModel) :
				new TypeExciteCondition(model, categoryModel));
	}

	/**
	 * 获取中途和结束触发的总产出
	 *
	 * @return
	 */
	public int getOverMultiple(DataWrap.Type type) {

		Map<Integer, IExciteCondition> map = conditionMap.get(type);
		if (null == map) {
			return 1;
		}

		Collection<IExciteCondition> conditions = FilterUtil.filter(map.values(), (cdt) -> cdt.triggerType() != ETriggerType.START);

		int out = 0;
		for (IExciteCondition cdt : conditions) {
			int currentValue = cardCategoryMap.getOrDefault(cdt.cardTypeValue(), 0);
			if (cdt.isDone(currentValue)) {
				out += cdt.model().getOutput();
			}
		}

		return out;
	}

	/**
	 * 牌型相关的[起手牌性不需要展示]
	 *
	 * @return
	 * @gameId
	 */
	public List<CardCategoryProto> toCardCategoryPBBuilder(int gameId) {
		if (cardCategoryMap.isEmpty()) {
			return Collections.emptyList();
		}
		//TODO 过滤clientShow字段
		List<CardCategoryProto> list = Lists.newArrayListWithCapacity(cardCategoryMap.size());
		cardCategoryMap.forEach((k, v) -> {
			//余牌特殊
			String cardCategory = ECardCategory.SURPLUS.cardTypeValue() == k ?
					ECardCategory.SURPLUS.cardCategory() :
					String.join(":", Integer.toString(gameId), Long.toString(k));

			list.add(CardCategoryProto.newBuilder().setCategory(cardCategory).setValue(v).build());
		});
		return list;
	}

	/**
	 * 牌型值
	 *
	 * @return
	 */
	public List<CommonLI> toCardCategoryValueList() {
		if (cardCategoryMap.isEmpty()) {
			return Collections.emptyList();
		}
		List<CommonLI> list = Lists.newArrayListWithCapacity(cardCategoryMap.size());
		cardCategoryMap.forEach((k, v) -> list.add(CommonLI.newBuilder().setK(k).setV(v).build()));
		return list;
	}

	/**
	 * 余牌数量，不一定有
	 *
	 * @return 如果没有余牌记录，反馈null
	 */
	public Integer getSurplusCardValue() {
		return cardCategoryMap.get(ECardCategory.SURPLUS.cardCategory());
	}

	/**
	 * 清除余牌数量
	 */
	public void deleteSurplusCardValue() {
		cardCategoryMap.remove(ECardCategory.SURPLUS.cardCategory());
	}

	/**
	 * @param cardArray
	 * @return
	 */
	final static Optional<CoinProtocol.CardArrayProto> toCardPB(int[] cardArray) {
		if (null == cardArray) {
			return Optional.empty();
		}
		CoinProtocol.CardArrayProto.Builder builder = CoinProtocol.CardArrayProto.newBuilder();
		for (int i = 0; i < cardArray.length; i++) {
			builder.addItem(cardArray[i]);
		}

		return Optional.of(builder.build());
	}
}
