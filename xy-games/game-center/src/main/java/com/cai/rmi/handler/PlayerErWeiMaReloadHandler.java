package com.cai.rmi.handler;

import java.util.HashMap;
import java.util.List;

import com.cai.common.constant.RMICmd;
import com.cai.common.domain.ClientUploadErWeiMaModel;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.dictionary.ClientPlayerErWeiMaDict;

/**
 * 通知加载玩家二维码RMI
 * @author wuhaoran
 */
@IRmi(cmd = RMICmd.BACKGROUND_RELOAD_PLAYER_ERWEIMA, desc = "通知加载玩家二维码")
public final class PlayerErWeiMaReloadHandler extends IRMIHandler<HashMap<String, String>, Integer> {

	@Override
	protected Integer execute(HashMap<String, String> map) {
		try {
			int id = Integer.valueOf(map.get("id").toString());
			long accountId = Long.valueOf(map.get("accountId").toString());
			int updateStatus = Integer.valueOf(map.get("updateStatus").toString());
			//后台审核后更新缓存
			List<ClientUploadErWeiMaModel> list = ClientPlayerErWeiMaDict.getInstance().getErWeiMaList(accountId);
			for (ClientUploadErWeiMaModel model : list) {
				if (model.getId() == id) {
					model.setUploadStatus(updateStatus);
				}
			}
			//ClientPlayerErWeiMaDict.getInstance().load();
		} catch (Exception e) {
			logger.error("find account erweima image error", e);
		}
		return 1;
	}

}
