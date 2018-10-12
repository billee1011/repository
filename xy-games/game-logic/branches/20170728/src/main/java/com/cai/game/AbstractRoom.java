package com.cai.game;

import com.cai.common.domain.Player;
import com.cai.common.domain.Room;
import com.cai.service.PlayerServiceImpl;

/**
 * 
 *
 * @author wu_hc date: 2017年7月27日 上午11:05:14 <br/>
 */
public abstract class AbstractRoom extends Room {

	/**
	 * 
	 */
	private static final long serialVersionUID = 839417244504630169L;

	public AbstractRoom(RoomType roomType, int maxNumber) {
		super(roomType, maxNumber);
	}

	@Override
	public boolean handler_enter_room_observer(Player player) {
		return super.handler_enter_room_observer(player);
	}

	@Override
	public boolean handler_exit_room_observer(Player player) {
		if (observers().exist(player.getAccount_id())) {
			observers().exit(player.getAccount_id());
			PlayerServiceImpl.getInstance().getPlayerMap().remove(player.getAccount_id());
			return true;
		}
		return false;
	}
}
