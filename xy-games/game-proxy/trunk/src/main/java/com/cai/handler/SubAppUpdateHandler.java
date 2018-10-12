/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.handler;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.cai.common.domain.AppItem;
import com.cai.dictionary.AppItemDict;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.RequestType;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.SubAppUpdateItem;
import protobuf.clazz.Protocol.SubAppUpdateRequest;
import protobuf.clazz.Protocol.SubAppUpdateResponse;

/**
 *
 * @author tb
 */
@ICmd(code = RequestType.SUBAPP_UPDATE_VALUE, exName = "subAppUpdateRequest")
public class SubAppUpdateHandler extends IClientHandler<SubAppUpdateRequest> {

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.xianyi.framework.cmd.IClientHandler#execute(com.google.protobuf.
	 * GeneratedMessage, com.cai.domain.Session)
	 */
	@Override
	protected void execute(SubAppUpdateRequest request, Request topRequest, C2SSession session) throws Exception {
		if (!request.hasAppId())
			return;

		int appId = request.getAppId();

		if (session.getAccount() == null)
			return;
		String version = request.getVersion();
		List<AppItem> list = AppItemDict.getInstance().getAppItemList();
		if (list.size() == 0) {
			return;
		}
		List<AppItem> cyItem = new ArrayList<AppItem>();
		int gameSeq = 1;
		List<AppItem> cyList = null;
		for (AppItem appItem : list) {
			if (appId == appItem.getAppId()) {
				if (version.equals(appItem.getVersions())) {
					return;
				} else {
					cyList = AppItemDict.getInstance().getAppItemDictMap().get(appId);
					for (AppItem item : cyList) {
						if(version.equals(item.getVersions())){
							gameSeq = item.getGameSeq();
							break;
						}
					}
					break;
				}
			} 
		}
		if(cyList!=null){
			for (AppItem item : cyList) {
				if (gameSeq < item.getGameSeq()) {
					cyItem.add(item);
				}
			}
		}
		SubAppUpdateResponse.Builder subAppUpdateResponse = SubAppUpdateResponse.newBuilder();
		for (AppItem item : cyItem) {
			if (StringUtils.isBlank(item.getDownUrl())) {
				continue;
			}
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
		session.send(responseBuilder.build());
	}

}
