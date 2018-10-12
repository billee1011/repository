/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.domain.AppItem;
import com.cai.common.util.PBUtil;
import com.cai.dictionary.AppItemDict;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Common.CommonIS;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.c2s.C2SProto.AppItemMiniProto;

/**
 * 
 * 
 *
 * @author wu_hc date: 2018年3月27日 下午4:26:08 <br/>
 */
@ICmd(code = C2SCmd.APPITEM_MINI, desc = "appitem icon,download url")
public final class AppItemMiniHandler extends IClientHandler<AppItemMiniProto> {

	private static final int ICON = 1;
	private static final int DOWNLOAD = 2;

	@Override
	protected void execute(AppItemMiniProto req, Request topRequest, C2SSession session) throws Exception {
		session.send(PBUtil.toS2CCommonRsp(S2CCmd.APP_ITEM_MINI, processAppReq(req)));
	}

	/**
	 * 
	 * @param builder
	 * @param req
	 */
	private AppItemMiniProto.Builder processAppReq(final AppItemMiniProto req) {

		AppItemMiniProto.Builder builder = AppItemMiniProto.newBuilder();
		builder.setCategory(req.getCategory());
		final List<AppItem> appItems = AppItemDict.getInstance().getAppItemList();
		if (req.getDatasCount() == 0) {
			appItems.forEach((item) -> {
				builder.addDatas(CommonIS.newBuilder().setK(item.getAppId()).setV(req.getCategory() == 1 ? item.getIconUrl() : item.getPackageDownPath()));
			});
		} else {

			final Map<Integer, AppItem> itemMaps = appItems.stream().collect(Collectors.toMap(AppItem::getAppId, a -> a));

			req.getDatasList().forEach((data) -> {
				AppItem item = itemMaps.get(data.getK());
				if (null != item) {
					if (req.getCategory() == ICON) {
						builder.addDatas(CommonIS.newBuilder().setK(item.getAppId()).setV(item.getIconUrl()));
					} else if (req.getCategory() == DOWNLOAD) {
						builder.addDatas(CommonIS.newBuilder().setK(item.getAppId()).setV(item.getPackageDownPath()));
					}
				}
			});
		}

		return builder;
	}
}
