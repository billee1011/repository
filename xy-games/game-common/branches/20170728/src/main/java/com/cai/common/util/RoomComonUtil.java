package com.cai.common.util;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.Room;

public class RoomComonUtil {
	
	public static int getMaxNumber(int _game_type_index,int _game_rule_index) {
		int game_type_index=_game_type_index;
		int maxNumber = 4;
		if (game_type_index == GameConstants.GAME_TYPE_FLS_LX_TWENTY || game_type_index == GameConstants.GAME_TYPE_HH_YX
				|| game_type_index == GameConstants.GAME_TYPE_PHZ_YX) {
			maxNumber = 3;
		}
		if(game_type_index==GameConstants.GAME_TYPE_FLS_CS_LX) {
			if(FvMask.has_any(_game_rule_index, FvMask.mask(GameConstants.GAME_RULE_HUNAN_THREE))) {
				maxNumber = 3;
			}
		}
		
		if(game_type_index==GameConstants.GAME_TYPE_HENAN|| game_type_index==GameConstants.GAME_TYPE_HENAN_LYGC 
				|| game_type_index==GameConstants.GAME_TYPE_ZZ|| game_type_index==GameConstants.GAME_TYPE_HENAN_ZHUAN_ZHUAN  
				||game_type_index==GameConstants.GAME_TYPE_HENAN_ZMD||game_type_index==GameConstants.GAME_TYPE_HENAN_KF
				||game_type_index==GameConstants.GAME_TYPE_HENAN_NY||game_type_index==GameConstants.GAME_TYPE_HENAN_XX
				||game_type_index==GameConstants.GAME_TYPE_HENAN_PDS||game_type_index==GameConstants.GAME_TYPE_HENAN_XY) {
			if(FvMask.has_any(_game_rule_index, FvMask.mask(GameConstants.GAME_RULE_HENAN_THREE))) {
				maxNumber = 3;
			}
		}
		
		if(game_type_index == GameConstants.GAME_TYPE_SEVER_OX||game_type_index == GameConstants.GAME_TYPE_SZOX
				||game_type_index == GameConstants.GAME_TYPE_LZOX||game_type_index == GameConstants.GAME_TYPE_ZYQOX
				||game_type_index == GameConstants.GAME_TYPE_MSZOX||game_type_index == GameConstants.GAME_TYPE_MFZOX
				||game_type_index == GameConstants.GAME_TYPE_TBOX
				||game_type_index == GameConstants.GAME_TYPE_SEVER_OX_LX||game_type_index == GameConstants.GAME_TYPE_SZOX_LX
				||game_type_index == GameConstants.GAME_TYPE_LZOX_LX||game_type_index == GameConstants.GAME_TYPE_ZYQOX_LX
				||game_type_index == GameConstants.GAME_TYPE_MSZOX_LX||game_type_index == GameConstants.GAME_TYPE_MFZOX_LX
				||game_type_index == GameConstants.GAME_TYPE_TBOX_LX)
		{
			maxNumber = GameConstants.GAME_PLAYER_OX;
		}
		if(game_type_index == GameConstants.GAME_TYPE_BTZ_YY)
		{
			maxNumber = GameConstants.GAME_PLAYER_BTZ;
		}
		if(game_type_index == GameConstants.GAME_TYPE_HJK){
			maxNumber = GameConstants.GAME_PLAYER_HJK;
		}
		if(game_type_index == GameConstants.GAME_TYPE_PDK_FP||game_type_index == GameConstants.GAME_TYPE_PDK_JD
				||game_type_index == GameConstants.GAME_TYPE_PDK_LZ||game_type_index == GameConstants.GAME_TYPE_PDK_SW)
		{
			maxNumber = 3;
			if(FvMask.has_any(_game_rule_index, FvMask.mask(GameConstants.GAME_RULE_TWO_PLAY))) {
				maxNumber = 2;
			}
			if(FvMask.has_any(_game_rule_index, FvMask.mask(GameConstants.GAME_RULE_THREE_PLAY))) {
				maxNumber = 3;
			}
			if(FvMask.has_any(_game_rule_index, FvMask.mask(GameConstants.GAME_RULE_FOUR_PLAY))) {
				maxNumber = 4;
			}
		}
		
		if(game_type_index == GameConstants.GAME_TYPE_SEVER_OX||game_type_index == GameConstants.GAME_TYPE_SZOX
				||game_type_index == GameConstants.GAME_TYPE_LZOX||game_type_index == GameConstants.GAME_TYPE_ZYQOX
				||game_type_index == GameConstants.GAME_TYPE_MSZOX||game_type_index == GameConstants.GAME_TYPE_MFZOX
				||game_type_index == GameConstants.GAME_TYPE_TBOX
				||game_type_index == GameConstants.GAME_TYPE_SEVER_OX_LX||game_type_index == GameConstants.GAME_TYPE_SZOX_LX
				||game_type_index == GameConstants.GAME_TYPE_LZOX_LX||game_type_index == GameConstants.GAME_TYPE_ZYQOX_LX
				||game_type_index == GameConstants.GAME_TYPE_MSZOX_LX||game_type_index == GameConstants.GAME_TYPE_MFZOX_LX
				||game_type_index == GameConstants.GAME_TYPE_TBOX_LX)
		{
			maxNumber = GameConstants.GAME_PLAYER_OX;
		}
		if(game_type_index == GameConstants.GAME_TYPE_BTZ_YY)
		{
			maxNumber = GameConstants.GAME_PLAYER_BTZ;
		}
		if(game_type_index == GameConstants.GAME_TYPE_HJK){
			maxNumber = GameConstants.GAME_PLAYER_HJK;
		}
		
		return maxNumber;
	}
	
	public static int getMaxNumber(Room table) {
		return getMaxNumber(table._game_type_index,table._game_rule_index);
		
	}

}
