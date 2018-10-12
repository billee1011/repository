package com.cai.game.fls.handler.flsdp;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.SysParamModel;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.DispatchCardRunnable;
import com.cai.future.runnable.XiPaiRunnable;
import com.cai.game.fls.FLSTable;
import com.cai.game.fls.handler.FLSHandler;
import com.cai.game.fls.handler.lxfls.FLSHandlerXiPai_FLS;

import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;

public class FLSHandlerXiPai_FLS_DP extends FLSHandlerXiPai_FLS {

	@Override
	public void exe(FLSTable table) {
		
		table.getLocationTip();
		
		SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(table.getGame_id())
				.get(5005);
		
		//洗牌动画 val1(服务器用总时间 毫秒) val2(每个牌发牌时间 毫秒) val3(每个牌移动时间 毫秒) val4(1-开启0-关闭)
		boolean open = false;
		int costTime = 0;
		if(sysParamModel!=null) {
			open=sysParamModel.getVal4()==1?true:false;
			costTime=sysParamModel.getVal1();
		}
		
		if(open) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_REFRESH_ROOM_STATUS);
			table._game_status = GameConstants.GS_MJ_XIPAI;// 设置状态
			table.load_room_info_data(roomResponse);
			
			int hand_cards[] = new int[GameConstants.MAX_FLS_COUNT];
			// 发送数据
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				roomResponse.clearCardData();
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], hand_cards);
				
				for(int j = 0;  j< hand_card_count; j++){
					roomResponse.addCardData(hand_cards[j]);
				}
				
				table.send_response_to_player(i, roomResponse);
			}
		}
		
		if(!open) costTime=0;//保险一点 
		
		
		GameSchedule.put(new XiPaiRunnable(table.getRoom_id()), costTime,
				TimeUnit.MILLISECONDS);
	}

	@Override
	public boolean handler_player_be_in_room(FLSTable table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		// 游戏变量
		if (table._shang_zhuang_player != GameConstants.INVALID_SEAT) {
			tableResponse.setBankerPlayer(table._shang_zhuang_player);
		} else if (table._lian_zhuang_player != GameConstants.INVALID_SEAT) {
			tableResponse.setBankerPlayer(table._lian_zhuang_player);
		} else {
			tableResponse.setBankerPlayer(GameConstants.INVALID_SEAT);
		}
		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		return true;
	}

}
