/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.game.manager;

import java.util.Collection;
import java.util.Map;

import com.game.Player;
import com.google.common.collect.Maps;

/**
 * 
 *
 * @author wu_hc date: 2017年10月11日 下午4:24:41 <br/>
 */
public final class PlayerMananger {

	private static PlayerMananger M = new PlayerMananger();

	private Map<Long, Player> players = Maps.newConcurrentMap();

	public static PlayerMananger getInstance() {
		return M;
	}

	public void addPlayer(final Player player) {
		players.put(player.getAccountId(), player);
	}

	public void removePlayer(long accountid) {
		players.remove(accountid);
	}

	public Player getPlayer(final long acccountId) {
		return players.get(acccountId);
	}

	public Collection<Player> players() {
		return players.values();
	}
}
