package com.lingyu.admin.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.lingyu.admin.core.ErrorCode;
import com.lingyu.admin.dao.AnnounceDao;
import com.lingyu.admin.network.AsyncHttpClient;
import com.lingyu.admin.network.GameClientManager;
import com.lingyu.admin.util.SessionUtil;
import com.lingyu.admin.vo.AnnounceVo;
import com.lingyu.common.entity.Announce;
import com.lingyu.common.entity.GameArea;
import com.lingyu.common.entity.Platform;
import com.lingyu.common.entity.User;
import com.lingyu.msg.http.AnnounceDelete_C2S_Msg;
import com.lingyu.msg.http.Announce_C2S_Msg;
import com.lingyu.msg.http.VersionNotic_C2S_Msg;

@Service
@Transactional(propagation = Propagation.REQUIRED)
public class AnnounceManager {
	private static final Logger logger = LogManager.getLogger(AnnounceManager.class);

	@Autowired
	private PlatformManager platformManager;
	@Autowired
	private GameAreaManager gameAreaManager;
	@Autowired
	private UserManager userManager;
	@Autowired
	private AnnounceDao announceDao;
	@Autowired
	private GameClientManager gameClientManager;
	
	private Map<Integer, Announce> id2announce;
	
	private Map<Integer, Announce> id2History;
	
	private enum AnnounceState{
		HISTORY,
		RUNNING,
		FUTURE,
	}
	
	public void init() {
		logger.info("公告缓存化开始");
		id2announce = new HashMap<>();
		id2History = new HashMap<>();
		List<Announce> list = announceDao.queryAll();
		for (Announce announce : list) {
			addAnnounce(announce);
		//	logger.debug("announce={}", announce.toString());
		}
		logger.info("公告缓存化完毕");
	}
	
	public Announce create(String content, int interval, Date startTime, Date endTime, boolean all, List<Integer> areaList){
		User user = SessionUtil.getCurrentUser();
		String pid = user.getLastPid();
		Collection<GameArea> areas = gameAreaManager.getHandleGameAreaList(pid, all, areaList);
		Set<Integer> set = new HashSet<>();
		if(all){
			set.add(-1);
		}else if(CollectionUtils.isEmpty(areas)){
			return null;
		}else{
			for(GameArea gameArea : areas){
				set.add(gameArea.getAreaId());
			}
		}
		
		Announce ret = new Announce();
		ret.setContent(content);
		ret.setInterval(interval);
		ret.setBeginTime(startTime);
		ret.setEndTime(endTime);
		ret.setPid(pid);
		ret.setUserId(user.getId());
		ret.setPf("");
		
		ret.setAreaIdSet(set);
		ret.serialize();
		String errorCode=announceDao.add(ret);
		if(errorCode.equals(ErrorCode.EC_OK)){
			addAnnounce(ret);
			
			if(calAnnounceState(ret) != AnnounceState.HISTORY){
				addAnnounceToGameServer(ret);
			}
		}
		
		
		logger.info("addAnnounce: admin={}, content={}, interval={}, all={}, pf={}", user.getName(), content, interval, all, "");
		
		return ret;
	}
	
	private void addAnnounce(Announce announce){
		if(calAnnounceState(announce) != AnnounceState.HISTORY){
			id2announce.put(announce.getId(), announce);
		}else{
			id2History.put(announce.getId(), announce);
		}
	}
	
	private boolean isValidTime(Announce announce){
		return announce.getBeginTime().before(announce.getEndTime());
	}
	
	private AnnounceState calAnnounceState(Announce announce){
		if(announce.isExists()){
			if(!isValidTime(announce)){
				return AnnounceState.HISTORY;
			}
			Date now = new Date();
			if(now.after(announce.getBeginTime()) && now.before(announce.getEndTime())){
				return AnnounceState.RUNNING;
			}
			if(now.before(announce.getBeginTime())){
				return AnnounceState.FUTURE;
			}
		}
		return AnnounceState.HISTORY;
	}
	
	public List<AnnounceVo> getAnnouncingList() {
		User user = SessionUtil.getCurrentUser();
		List<Announce> ret = new ArrayList<Announce>();
		List<Integer> moveList = new ArrayList<>();
		for(Announce announce : id2announce.values()){
			AnnounceState state = calAnnounceState(announce);
			if(state != AnnounceState.HISTORY){
				if(announce.getPid().equals(user.getLastPid())){
					ret.add(announce);
				}
			}else{
				moveList.add(announce.getId());
			}
		}
		
		for(Integer id : moveList){
			moveToHistory(id);
		}
		
		return toAnnounceVoList(ret);
	}
	
	private List<AnnounceVo> toAnnounceVoList(List<Announce> announces){
		List<AnnounceVo> ret = new ArrayList<>(announces.size());
		for(Announce announce : announces){
			ret.add(toAnnounceVo(announce));
		}
		return ret;
	}
	
	private AnnounceVo toAnnounceVo(Announce announce){
		AnnounceVo ret = new AnnounceVo();
		ret.setId(announce.getId());
		ret.setInterval(announce.getInterval());
		ret.setBeginTime(announce.getBeginTime());
		ret.setEndTime(announce.getEndTime());
		
		if("-1".equals(announce.getAreaIds())){
			ret.setAreaNames("所有区服");
		}else{
			StringBuilder sb = new StringBuilder();
			Set<Integer> set = announce.getAreaIdSet();
			int index = 0;
			for(Integer areaId : set){
				if(index > 0){
					sb.append(",");
				}
				GameArea gameArea = gameAreaManager.getGameAreaByAreaId(announce.getPid(),areaId);
				sb.append(gameArea != null? gameArea.getAreaId():"未知区服");
				index++;
			}
			ret.setAreaNames(sb.toString());
		}
		
		ret.setContent(announce.getContent());
		ret.setExists(announce.isExists());
		Platform platform = platformManager.getPlatform(announce.getPid());
		ret.setPlatformName(platform != null? platform.getName():"未知平台");
		User user = userManager.getUser(announce.getUserId());
		ret.setUserName(user != null? user.getName():"未知管理员");
		return ret;
	}
	
	public List<AnnounceVo> getAnnounceHistory() {
		User user = SessionUtil.getCurrentUser();
		List<Announce> ret = new ArrayList<Announce>();
		for(Announce announce : id2History.values()){
			if(announce.getPid().equals(user.getLastPid())){
				ret.add(announce);
			}
		}
		return toAnnounceVoList(ret);
	}
	
	public Announce getAnnounce(int id){
		return id2announce.get(id);
	}

	public boolean deleteAnnounce(int id) {
		Announce announce = id2announce.get(id);
		if(announce != null){
			announce.setExists(false);
			announceDao.update(announce);
			moveToHistory(id);
			deleteAnnounceOfGameServer(announce);
			User admin = SessionUtil.getCurrentUser();
			logger.info("deleteAnnounce: admin={}, announceId={}, content={}, interval={}, areaIds={}", admin.getName(), id, announce.getContent(), announce.getInterval(), announce.getAreaIds());
			return true;
		}
		return false;
	}

	private void moveToHistory(int announceId){
		Announce announce = id2announce.remove(announceId);
		id2History.put(announceId, announce);
	}

	private void addAnnounceToGameServer(Announce announce) {
		Collection<GameArea> areas = getGameAreas(announce);
		
		Announce_C2S_Msg msg = new Announce_C2S_Msg();
		msg.setId(announce.getId());
		msg.setBeginTime(announce.getBeginTime());
		msg.setEndTime(announce.getEndTime());
		msg.setInterval(announce.getInterval());
		msg.setContent(announce.getContent());
		msg.setPf(announce.getPf());
		AsyncHttpClient.getInstance().send(areas, msg);
	}
	
	private void deleteAnnounceOfGameServer(Announce announce){
		Collection<GameArea> areas = getGameAreas(announce);
		
		AnnounceDelete_C2S_Msg msg = new AnnounceDelete_C2S_Msg();
		msg.setId(announce.getId());
		AsyncHttpClient.getInstance().send(areas, msg);
	}
	
	private Collection<GameArea> getGameAreas(Announce announce){
		Collection<GameArea> areas = null;
		if("-1".equals(announce.getAreaIds())){
			areas = gameAreaManager.getGameAreaList(announce.getPid());
		}else{
			areas = gameAreaManager.getGameAreaListByIds(announce.getPid(),announce.getAreaIdSet());
		}
		return areas;
	}
	
	/**
	 * 添加公告
	 * @param type
	 * @param content
	 */
	public void addNotice(int type, String content, String version) {
		VersionNotic_C2S_Msg msg = new VersionNotic_C2S_Msg();
		msg.setType(type);
		msg.setContent(content);
		msg.setVersion(version);
		msg.setTime(new Date());
		
		gameClientManager.getCurrentGameClient().versionNotic(msg);
	}
	
}