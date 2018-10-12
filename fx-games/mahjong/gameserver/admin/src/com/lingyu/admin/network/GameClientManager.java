package com.lingyu.admin.network;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lingyu.admin.manager.GameAreaManager;
import com.lingyu.admin.util.SessionUtil;
import com.lingyu.common.core.ServiceException;
import com.lingyu.common.entity.GameArea;
import com.lingyu.common.entity.User;

@Service
public class GameClientManager {
	
	@Autowired
	private GameAreaManager gameAreaManager;

	private Map<Integer, GameClient> gameClientMap = new HashMap<Integer, GameClient>();
	
	public void destroyAllClient() {
		AsyncHttpClient.getInstance().destory();
		Collection<GameClient> col = gameClientMap.values();
		for (GameClient e : col) {
			e.destory();
		}
	}
	
	/**
	 * 获取当前游戏端
	 * @return
	 * @throws ServiceException
	 */
	public GameClient getCurrentGameClient() throws ServiceException {
		int areaId = SessionUtil.getCurrentAreaId();
		if (areaId == 0) {
			throw new ServiceException("没有选择游戏区");
		}
		User user = SessionUtil.getCurrentUser();
		if (user == null) {
			throw new ServiceException("当前用户没有登录");
		}
		return getGameClientByAreaId(user.getLastPid(), areaId);
	}
	
	public GameClient getGameClient(int worldId) {
		GameClient ret = null;
		GameArea gameArea = gameAreaManager.getGameArea(worldId);
		if (gameArea != null) {
			ret = this.getGameClientByAreaId(gameArea.getPid(), gameArea.getAreaId());
		}
		return ret;
	}
	
	public GameClient getGameClientByAreaId(String pid, int areaId) throws ServiceException {
		GameClient client = gameClientMap.get(areaId);
		if (client == null) {
			GameArea gameArea = gameAreaManager.getGameAreaByAreaId(pid, areaId);
			if (gameArea == null) {
				throw new ServiceException("找不到游戏区" + areaId);
			}
			client = new GameClient(gameArea.getIp(), gameArea.getPort(), 16000);
			// if (client != null
			// && client.getPdlClientPool().getResource().isConnected()) {
			gameClientMap.put(areaId, client);
			// }

		}
		return client;
	}
}
