package com.cai.dictionary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.domain.ClientUploadErWeiMaModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.core.Global;
import com.cai.service.PublicService;

/**
 * 玩家二维码字典
 *
 */
public class ClientPlayerErWeiMaDict {

	private Logger logger = LoggerFactory.getLogger(ClientPlayerErWeiMaDict.class);

	private Map<Long, List<ClientUploadErWeiMaModel>> clientPlayerErWeiMaMap;

	/**
	 * 单例
	 */
	private static ClientPlayerErWeiMaDict instance;

	/**
	 * 私有构造
	 */
	private ClientPlayerErWeiMaDict() {
		clientPlayerErWeiMaMap = new HashMap<>();
	}

	/**
	 * 单例模式
	 * 
	 * @return 字典单例
	 */
	public static ClientPlayerErWeiMaDict getInstance() {
		if (null == instance) {
			instance = new ClientPlayerErWeiMaDict();
		}

		return instance;
	}

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		PublicService publicService = SpringService.getBean(PublicService.class);
		List<ClientUploadErWeiMaModel> erweimaList = publicService.getPublicDAO().getClientUploadErWeiMaModelList();
		Map<Long, List<ClientUploadErWeiMaModel>> dataMap = new HashMap<>();
		for (ClientUploadErWeiMaModel model : erweimaList) {
			if (dataMap.get(model.getAccountId()) != null) {
				List<ClientUploadErWeiMaModel> curList = dataMap.get(model.getAccountId());
				curList.add(model);
			} else {
				List<ClientUploadErWeiMaModel> addList = new ArrayList<>();
				addList.add(model);
				dataMap.put(model.getAccountId(), addList);
			}
		}
		this.clientPlayerErWeiMaMap = dataMap;
		logger.info("load ClientPlayerErWeiMaDict success! " + timer.getStr());
	}

	//新增
	public void add(long accountId, String imageUrl, int uploadType){
		PublicService publicService = SpringService.getBean(PublicService.class);
		ClientUploadErWeiMaModel erweima = new ClientUploadErWeiMaModel();
		erweima.setAccountId(accountId);
		erweima.setImage(imageUrl);
		erweima.setUploadStatus(uploadType);
		erweima.setUploadTime(new Date());
		try {
			//异步执行入库
			Global.getDbService().execute(new Runnable() {
				@Override
				public void run() {
					publicService.getPublicDAO().insertClientUploadErWeiMaModel(erweima);
				}
			});
			//更新缓存
			if(clientPlayerErWeiMaMap.get(accountId) == null) {
				List<ClientUploadErWeiMaModel> newList = new ArrayList<>();
				newList.add(erweima);
				clientPlayerErWeiMaMap.put(accountId, newList);
			} else {
				List<ClientUploadErWeiMaModel> curList = clientPlayerErWeiMaMap.get(accountId);
				curList.add(erweima);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//编辑
	public void update(int id, long accountId, String imageUrl, int uploadType){
		PublicService publicService = SpringService.getBean(PublicService.class);
		ClientUploadErWeiMaModel erweima = new ClientUploadErWeiMaModel();
		erweima.setId(id);
		erweima.setAccountId(accountId);
		erweima.setImage(imageUrl);
		erweima.setUploadStatus(uploadType);
		erweima.setUploadTime(new Date());
		try {
			//异步执行入库
			Global.getDbService().execute(new Runnable() {
				@Override
				public void run() {
					publicService.getPublicDAO().updateClientUploadErWeiMaModel(erweima);
				}
			});
			//更新缓存
			List<ClientUploadErWeiMaModel> curList = clientPlayerErWeiMaMap.get(accountId);
			for (ClientUploadErWeiMaModel model : curList) {
				if (model.getId() == erweima.getId()) {
					model.setImage(erweima.getImage());
					model.setUploadStatus(erweima.getUploadStatus());
					model.setUploadTime(erweima.getUploadTime());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//通过accounId获取二维码图片
	public List<ClientUploadErWeiMaModel> getErWeiMaList(long accountId){
		if (clientPlayerErWeiMaMap.get(accountId) == null) {
			return Collections.emptyList();
		}
		return clientPlayerErWeiMaMap.get(accountId);
	}
	
}
