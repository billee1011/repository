/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.service;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;

import com.cai.common.define.IDType;
import com.cai.common.domain.CoinCornucopiaModel;
import com.cai.common.domain.Event;
import com.cai.common.domain.RoomGeneratorModel;
import com.cai.common.id.BrandIDGenerator;
import com.cai.common.id.IDGenerator;
import com.cai.common.id.RoomIdGenerator;
import com.cai.common.util.MD5;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.common.util.XYGameException;
import com.cai.core.MonitorEvent;
import com.cai.domain.Session;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * 
 * ID 生成服务
 * 
 * @author wu_hc date: 2017年8月4日 下午2:21:46 <br/>
 */
public final class IDServiceImpl extends AbstractService {

	/**
	 * 
	 */
	private final Map<IDType, IDGenerator> generatorMap = new EnumMap<>(IDType.class);

	private final static IDServiceImpl imp = new IDServiceImpl();
	
	
	private final List<RoomGeneratorModel> roomGeneratorModels = Lists.newArrayList();
	
	
	private volatile boolean updateRoomGenerator =false;

	public static IDServiceImpl getInstance() {
		return IDServiceImpl.imp;
	}

	@Override
	protected void startService() {
		Random random = new Random(1537846558926L);//种子不能改
		generatorMap.put(IDType.BRAND, new BrandIDGenerator(MongoDBServiceImpl.getInstance().getMaxBrandId()));

		// 房间id----按功能区分房间范围，最大不要超过9999999
		generatorMap.put(IDType.NORMAL_ROOM, new RoomIdGenerator(idGenerate(110000, 999999, random), IDType.NORMAL_ROOM));
		generatorMap.put(IDType.COIN_ROOM, new RoomIdGenerator(idGenerate(1000000, 2000000, random), IDType.COIN_ROOM));
		
		
		
		
//		//还有类型往上写
		initRoomGeneratorModels();

		PublicService publicService = SpringService.getBean(PublicService.class);
		List<RoomGeneratorModel> roomGeneratorModelListFromDb = publicService.getPublicDAO().getRoomGeneratorList();
		if(roomGeneratorModelListFromDb!=null && !roomGeneratorModelListFromDb.isEmpty()) {
			checkRoomGeneratorModelDB(roomGeneratorModelListFromDb);
		}else {
			for(RoomGeneratorModel model:roomGeneratorModels) {//首次入库
				publicService.getPublicDAO().insertRoomGeneratorModel(model);
			}
		}
		
		updateRoomGenerator =true;
		
	}
	
	
	private String md5Arrays(int[] arrays) {
		return MD5.MD5Encode(Arrays.toString( arrays));
	}
	/**
	 * 检查数据库中的数据是否正确，初始化游标
	 * @param roomGeneratorModelListFromDb
	 * @return
	 */
	private void checkRoomGeneratorModelDB(List<RoomGeneratorModel> roomGeneratorModelListFromDb) {
		for(RoomGeneratorModel roomGeneratorModel:roomGeneratorModelListFromDb) {
			RoomIdGenerator roomIdGenerator  =(RoomIdGenerator)generatorMap.get(IDType.getIDType(roomGeneratorModel.getRoom_id_type()));
			boolean isEquals =md5Arrays(roomIdGenerator.getIdGroup()).equals(roomGeneratorModel.getRoomd_ids());
			if(!isEquals){
				logger.error("checkRoomGeneratorModelDB 生成的数据不一致，请检查");
				System.exit(-1);
			} 
		}
		
		//初始化索引值
		for(RoomGeneratorModel roomGeneratorModel:roomGeneratorModelListFromDb) {
			RoomIdGenerator roomIdGenerator  =(RoomIdGenerator)generatorMap.get(IDType.getIDType(roomGeneratorModel.getRoom_id_type()));
			roomIdGenerator.setCursor(roomGeneratorModel.getRoom_index()+2000);//当前值的基础上加2000
		}
	}
	
	
	/**
	 * 初始化要更新入库的数据
	 */
	private void initRoomGeneratorModels() {
		for(IDGenerator idGenerator:generatorMap.values()) {
			if(idGenerator instanceof RoomIdGenerator) {
				RoomGeneratorModel roomGeneratorModel = new RoomGeneratorModel();
				
				RoomIdGenerator roomIdGenerator = (RoomIdGenerator)idGenerator;
				roomGeneratorModel.setRoom_id_type(roomIdGenerator.getType().ordinal());
				roomGeneratorModel.setNeedDB(true);
				roomGeneratorModel.setRoom_index(roomIdGenerator.getCursor());
				roomGeneratorModel.setRoomd_ids(md5Arrays(roomIdGenerator.getIdGroup()));
				roomGeneratorModels.add(roomGeneratorModel);
			}
		}
	}
	
	/**
	 * 获取当然索引更新入库
	 * @return
	 */
	public List<RoomGeneratorModel> getRoomGeneratorModelToDB() {
		if(updateRoomGenerator) {//完成了初始化，检测完成才能更新
			for(RoomGeneratorModel roomGeneratorModel:roomGeneratorModels) {
				IDGenerator idGenerator = generatorMap.get(IDType.getIDType(roomGeneratorModel.getRoom_id_type()));
				if(idGenerator instanceof RoomIdGenerator) {
					roomGeneratorModel.setRoom_index(((RoomIdGenerator) idGenerator).getCursor());
				}
			}
			return roomGeneratorModels;
		}
	    return 	Lists.newArrayList();
	
	}

	public long next(IDType type) {
		IDGenerator idGenerator = generatorMap.get(type);
		if (null == idGenerator) {
			logger.error("严重BUG ============== 未配置ID生成器类型 ==================={}", type.name());
			return -1L;
		}
		return idGenerator.nextId();
	}

	public int nextInt(IDType type) {
		IDGenerator idGenerator = generatorMap.get(type);
		if (null == idGenerator) {
			logger.error("严重BUG ============== 未配置ID生成器类型 ==================={}", type.name());
			return -1;
		}
		return (int) idGenerator.nextId();
	}

	@Override
	public MonitorEvent montior() {
		return null;
	}

	@Override
	public void onEvent(Event<SortedMap<String, String>> event) {

	}

	@Override
	public void sessionCreate(Session session) {

	}

	@Override
	public void sessionFree(Session session) {

	}

	@Override
	public void dbUpdate(int _userID) {
	}

	/**
	 * 
	 * @param min
	 * @param max
	 * @param random
	 * @return
	 */
	private int[] idGenerate(int min, int max, Random random) {

		if (min < 0 || max < 0 || min >= max) {
			throw new XYGameException(String.format("id生成参数不合理！min:%d ,max:%d", min, max));
		}
		int len = max - min + 1;
		int[] ids = new int[len];
		for (int i = 0, idx = min; i < len; i++) {
			ids[i] = idx++;
		}

		RandomUtil.shuffle(ids, null != random ? random : new Random());
		return ids;
	}

	public static void main(String[] args) {
		IDServiceImpl imp = new IDServiceImpl();

		imp.startService();
		
		
//		Set<Integer> set = Sets.newHashSet();
//		for (int i = 0; i < 10000000; i++) {
//			set.add(imp.nextInt(IDType.NORMAL_ROOM));
//		}
//
//		System.out.println(System.currentTimeMillis());
//		
//		
//		 
		 for(int i=0;i<10;i++) {
//			 int[] arrays = new int[]{5,3,4,9,10};
//			 Random random = new Random(1000); 
//			 RandomUtil.shuffle(arrays, random);
//			 System.out.println(Arrays.toString(arrays));
			 
			 Random random = new Random(1537846558926L);//种子不能改
			 RoomIdGenerator roomIdGenerator =  new RoomIdGenerator(IDServiceImpl.getInstance().idGenerate(10, 99, random), IDType.NORMAL_ROOM);
			 
			 RoomIdGenerator roomIdGenerator2 =  new RoomIdGenerator(IDServiceImpl.getInstance().idGenerate(100, 150, random), IDType.COIN_ROOM);
			 
//			 System.out.println(Arrays.toString( roomIdGenerator.getIdGroup()));
//			 System.out.println(Arrays.toString( roomIdGenerator2.getIdGroup()));
			 
			 System.out.println( MD5.MD5Encode(Arrays.toString( roomIdGenerator.getIdGroup())));
		 }
//		
		
		 
	}
}
