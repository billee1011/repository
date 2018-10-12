package com.cai.game.yyqf.handler;

import com.cai.game.yyqf.YYQFTable;

public class YYQFHandlerFinish extends YYQFHandler {

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.fls.handler.FLSHandler#exe(com.cai.fls.FLSTable)
	 */
	@Override
	public void exe(YYQFTable table) {
	}

	@Override
	public boolean handler_player_be_in_room(YYQFTable table, int seat_index) {
		if (table.saveEndResponse != null)
			table.send_response_to_player(seat_index, table.saveEndResponse);
		return true;
	}

}
