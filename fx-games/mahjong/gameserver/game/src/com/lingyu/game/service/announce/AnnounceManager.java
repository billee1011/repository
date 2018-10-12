package com.lingyu.game.service.announce;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lingyu.common.core.ErrorCode;
import com.lingyu.common.entity.Announce;
import com.lingyu.common.io.MsgType;
import com.lingyu.common.util.TimeUtil;
import com.lingyu.game.RouteManager;
import com.lingyu.game.service.id.IdManager;
import com.lingyu.game.service.id.TableNameConstant;
import com.lingyu.game.service.mahjong.MahjongConstant;
import com.lingyu.noark.data.repository.QueryFilter;

@Service
public class AnnounceManager {
	private static final Logger logger = LogManager.getLogger(AnnounceManager.class);
	@Autowired
	private AnnounceRepository announceRepository;
	@Autowired
	private RouteManager routeManager;
	@Autowired
	private IdManager idManager;

	/**
	 * 添加公告
	 *
	 * @param id
	 * @param content
	 * @param beginTime
	 * @param endTime
	 * @param interval
	 * @param pf
	 */
	public void addAnnounce(int id, String content, Date beginTime, Date endTime, int interval) {
		Announce announce = new Announce();
		announce.setId(idManager.newId(TableNameConstant.ANNOUNCE));
		announce.setAnnounceId(id);
		announce.setContent(content);
		announce.setBeginTime(beginTime);
		announce.setEndTime(endTime);
		announce.setInterval(interval);
		announceRepository.cacheInsert(announce);

		JSONObject res = new JSONObject();
		res.put(ErrorCode.RESULT, ErrorCode.OK);
		res.put(MahjongConstant.CLIENT_DATA, toAnnounceVo(announce));
		routeManager.broadcast(MsgType.Announce_AddOrUpdate_Msg, res);

		logAnnounce(announce, "addAnnounce");
	}

	private void logAnnounce(Announce announce, String key) {
		logger.info("{}: id={}, announceId={}, content={}, interval={}", key, announce.getId(),
		        announce.getAnnounceId(), announce.getContent(), announce.getInterval());
	}

	private Object[] toAnnounceVo(Announce announce) {
		return new Object[] { announce.getId(), announce.getContent(), announce.getInterval(),
		        DateFormatUtils.format(announce.getBeginTime(), TimeUtil.PATTERN_yyyy_MM_dd_HH_mm_ss),
		        DateFormatUtils.format(announce.getEndTime(), TimeUtil.PATTERN_yyyy_MM_dd_HH_mm_ss) };
	}

	/**
	 * 删除指定公告
	 *
	 * @param id
	 */
	public void deleteAnnounceByAnnounceId(final int id) {
		List<Announce> list = announceRepository.cacheLoadAll(new QueryFilter<Announce>() {
			@Override
			public boolean stopped() {
				return false;
			}

			@Override
			public boolean check(Announce t) {
				return id == t.getAnnounceId();
			}
		});
		if (CollectionUtils.isNotEmpty(list)) {
			Announce announce = list.get(0);
			announceRepository.cacheDelete(announce);

			logAnnounce(announce, "deleteAnnounce");

			JSONObject jsonObject = new JSONObject();
			jsonObject.put("id", announce.getId());
			routeManager.broadcast(MsgType.Announce_Delete_Msg, jsonObject);
		}
	}

	/**
	 * 获取正在广播的公告和将来的公告
	 *
	 * @return
	 */
	private List<Announce> getAnnouncingOrFutureList() {
		List<Announce> ret = new ArrayList<>();
		List<Announce> list = announceRepository.cacheLoadAll();
		for (Announce announce : list) {
			if (isValidOrFutureAnnounce(announce)) {
				ret.add(announce);
			} else {
				announceRepository.cacheDelete(announce);
			}
		}
		return ret;
	}

	private boolean isValidOrFutureAnnounce(Announce announce) {
		Date now = new Date();
		return (now.after(announce.getBeginTime()) && now.before(announce.getEndTime()))
		        || now.before(announce.getBeginTime());
	}

	/**
	 * 获取正在广播的公告和将来的公告
	 *
	 * @return
	 */
	public JSONObject getAnnouncingOrFutureListMsg() {
		List<Announce> list = getAnnouncingOrFutureList();
		JSONArray array = new JSONArray();
		for (Announce announce : list) {
			array.add(toAnnounceVo(announce));
		}
		JSONObject result = new JSONObject();
		result.put(ErrorCode.RESULT, ErrorCode.EC_OK);
		result.put("data", array);
		return result;
	}
}
