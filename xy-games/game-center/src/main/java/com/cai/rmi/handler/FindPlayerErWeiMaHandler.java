package com.cai.rmi.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.cai.common.constant.RMICmd;
import com.cai.common.domain.ClientUploadErWeiMaModel;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.dictionary.ClientPlayerErWeiMaDict;

/**
 * 玩家二维码列表rmi
 * @author wuhaoran
 */
@IRmi(cmd = RMICmd.FIND_PLAYER_CLIENT_UPLOAD_ERWEIMA, desc = "玩家二维码列表")
public final class FindPlayerErWeiMaHandler extends IRMIHandler<HashMap<String, String>, List<ClientUploadErWeiMaModel>> {

	@Override
	protected List<ClientUploadErWeiMaModel> execute(HashMap<String, String> map) {
		List<ClientUploadErWeiMaModel> dataList = new ArrayList<>();
		try {
			long accountId = Long.valueOf(map.get("accountId").toString());
			dataList = ClientPlayerErWeiMaDict.getInstance().getErWeiMaList(accountId);
		} catch (Exception e) {
			logger.error("find account erweima image error", e);
		}
		return dataList;
	}

}
