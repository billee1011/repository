/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.future.runnable;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.cai.common.constant.RMICmd;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.SpringService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import protobuf.redis.ProtoRedis.RsDictUpdateResponse.RsDictType;

/**
 * 加载配置任务
 *
 * @author wu_hc date: 2018年4月7日 上午11:36:09 <br/>
 */
public final class DictReloadRunnable implements Runnable {

	static final Map<RsDictType, Predicate<ICenterRMIServer>> func = Maps.newHashMap();
	static {
		func.put(RsDictType.SYS_PARAM, (rmi) -> rmi.reLoadSysParamDict());
		func.put(RsDictType.SYS_NOTICE, (rmi) -> rmi.reLoadSysNoticeModelDictionary());

		func.put(RsDictType.GAME_DESC, (rmi) -> rmi.reLoadGameDescDictionary());

	}
	/**
	 * 需要加载的配置类型 {@link }
	 */
	private final List<Integer> dicts = Lists.newArrayList();

	/**
	 * 执行时间
	 */
	private final Date executeDate;

	public static final DictReloadRunnable newReloadTask(Date executeDate) {
		return new DictReloadRunnable(executeDate);
	}

	/**
	 * @param executeDate
	 */
	private DictReloadRunnable(Date executeDate) {

		this.executeDate = executeDate;
	}

	@Override
	public void run() {
		ICenterRMIServer service = SpringService.getBean(ICenterRMIServer.class);
		dicts.forEach(type -> {
			RsDictType dictType = RsDictType.valueOf(type);
			if (null != dictType) {
				Predicate<ICenterRMIServer> pre = func.get(dictType);
				if (null != pre) {
					pre.test(service);
				}
			} else {
				service.rmiInvoke(RMICmd.DICT_RELOAD, type);
			}
		});
	}

	public List<Integer> getDicts() {
		return dicts;
	}

	public Date getExecuteDate() {
		return executeDate;
	}

}
