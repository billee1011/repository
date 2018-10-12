/**
 * 
 */
package com.cai.handler;

import java.util.ArrayList;
import java.util.List;

import com.cai.common.domain.AppItem;
import com.cai.dictionary.AppItemDict;
import com.cai.net.core.ClientHandler;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.SubAppUpdateItem;
import protobuf.clazz.Protocol.SubAppUpdateRequest;
import protobuf.clazz.Protocol.SubAppUpdateResponse;

/**
 * @author tb
 *
 */
public class TestSubAppUpdateHandler extends ClientHandler<SubAppUpdateRequest> {

	@Override
	public void onRequest() throws Exception {
		if (!request.hasAppId())
			return;

		int appId = request.getAppId();

		// 操作频率控制
		if (!session.isCanRequest("SubAppUpdateHandler" + appId, 300L)) {
			return;
		}

		if (session.getAccount() == null)
			return;
		String version = request.getVersion();
		List<AppItem> list = AppItemDict.getInstance().getAppItemList();
		if (list.size() == 0) {
			return;
		}
		List<AppItem> cyItem = new ArrayList<AppItem>();
		for (AppItem appItem : list) {
			if (appId == appItem.getAppId()) {
				if (version.equals(appItem.getVersions())) {
					return;
				} else {
					int gameSeq = 0;
					List<AppItem> cyList = AppItemDict.getInstance().getAppItemDictMap().get(appId);
					for (AppItem item : cyList) {
						if (version.equals(item.getVersions())) {
							gameSeq = item.getGameSeq();
							break;
						}
					}
					for (AppItem item : cyList) {
						if (gameSeq < item.getGameSeq()) {
							cyItem.add(item);
						}
					}
					break;
				}
			} else {
				continue;
			}
		}
		SubAppUpdateResponse.Builder subAppUpdateResponse = SubAppUpdateResponse.newBuilder();
		for (AppItem item : cyItem) {
			SubAppUpdateItem.Builder subAppUpdateItem = SubAppUpdateItem.newBuilder();
			subAppUpdateItem.setAppId(item.getAppId());
			subAppUpdateItem.setDownUrl(item.getDownUrl());
			subAppUpdateItem.setSize(item.getSize());
			subAppUpdateItem.setVersion(item.getVersions());
			subAppUpdateResponse.addItems(subAppUpdateItem);
		}
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.SUBAPP_UPDATE);
		responseBuilder.setExtension(Protocol.subAppUpdateResponse, subAppUpdateResponse.build());
		send(responseBuilder.build());
	}

}
