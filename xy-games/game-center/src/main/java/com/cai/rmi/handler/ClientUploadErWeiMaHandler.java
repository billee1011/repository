package com.cai.rmi.handler;

import java.util.HashMap;
import java.util.List;

import com.cai.common.constant.RMICmd;
import com.cai.common.domain.ClientUploadErWeiMaModel;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.dictionary.ClientPlayerErWeiMaDict;

/**
 * 玩家在客户端上传二维码rmi
 * @author wuhaoran
 */
@IRmi(cmd = RMICmd.CLIENT_UPLOAD_ERWEIMA, desc = "玩家在客户端上传二维码")
public final class ClientUploadErWeiMaHandler extends IRMIHandler<HashMap<String, String>, HashMap<String, Object>> {

	@Override
	protected HashMap<String, Object> execute(HashMap<String, String> map) {
		HashMap<String, Object> resultMap = new HashMap<>();
		try {
			String isId = map.get("id");
			long accountId = Long.valueOf(map.get("accountId").toString());
			String imageUrl = map.get("imageUrl").toString();
			int uploadType = Integer.valueOf(map.get("uploadType").toString());
			if ("0".equals(isId)) {
				List<ClientUploadErWeiMaModel> curList = ClientPlayerErWeiMaDict.getInstance().getErWeiMaList(accountId);
				if (curList.size() > 2) {
					resultMap.put("result", 0);
					resultMap.put("msg", "该用户已经有两张图片了,不能在上传");
					return resultMap;
				}
				ClientPlayerErWeiMaDict.getInstance().add(accountId,imageUrl,uploadType); 	//新增
			} else {
				ClientPlayerErWeiMaDict.getInstance().update(Integer.valueOf(isId),accountId,imageUrl,uploadType);
			}
			//新增或修改完成后返回结果
			List<ClientUploadErWeiMaModel> dataList = ClientPlayerErWeiMaDict.getInstance().getErWeiMaList(accountId);
			resultMap.put("result", 1);
			resultMap.put("data", dataList);
		} catch (Exception e) {
			logger.error("addProxyConsumeStatistics error", e);
		}
		return resultMap;
	}

}
