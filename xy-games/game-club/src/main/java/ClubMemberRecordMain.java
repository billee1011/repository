import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.cai.common.domain.ClubMemberRecordModel;
import com.cai.common.util.AbstractServer;
import com.cai.common.util.PropertiesUtil;
import com.cai.common.util.SpringService;
import com.cai.service.ClubDaoService;
import com.xianyi.framework.core.transport.event.IOEventListener;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

public final class ClubMemberRecordMain extends AbstractServer {

	public static void main(String[] args) {

		ClubMemberRecordMain server = new ClubMemberRecordMain();
		try {
			// 1基础组件启动
			server.initConfig();
			server.startSpring();
			// ********执行前备份 club_member_record club_account

			final String extFileName = "delInfo.txt";
			final int delDay = 8;
			StringBuilder sb = new StringBuilder();
			Map<Integer, Map<Long, List<ClubMemberRecordModel>>> totalMap = initAllMemberRecord();
			List<ClubMemberRecordModel> delList = new ArrayList<>();
			Map<Integer, List<ClubMemberRecordModel>> delDayMap = new HashMap<>();
			for (int i = 1; i <= delDay; i++) {
				delDayMap.put(i, new ArrayList<>());
			}

			for (Map<Long, List<ClubMemberRecordModel>> map : totalMap.values()) {
				for (List<ClubMemberRecordModel> list : map.values()) {
					int day0_num = 0;
					Map<Integer, List<ClubMemberRecordModel>> playerMap = new HashMap<>();
					for (int i = 1; i <= delDay; i++) {
						playerMap.put(i, new ArrayList<>());
					}

					long maxId = 0;
					ClubMemberRecordModel totalModel = null;
					for (ClubMemberRecordModel model : list) {
						if (model.getDay() == 0) {
							day0_num++;
							if (model.getId() > maxId) {
								maxId = model.getId();
								totalModel = model;
							}
						} else {
							List<ClubMemberRecordModel> tmpList = playerMap.get(model.getDay());
							if (tmpList != null) {
								tmpList.add(model);
							}
						}
					}
					if (day0_num > 1 && totalModel != null) {
						int totalGameCount = 0;
						for (ClubMemberRecordModel model : list) {
							if (model.getId() > maxId) {
								totalGameCount += model.getGameCount();
							}
						}
						if (totalGameCount != totalModel.getGameCount()) {
							String msg = "====ERROR id=" + totalModel.getId() + " clubId=" + totalModel.getClubId() + " accountId="
									+ totalModel.getAccountId() + " day=" + totalModel.getDay();
							System.out.println(msg);
							sb.append(msg).append("\n");
						}
						for (ClubMemberRecordModel model : list) {
							if (model.getId() < maxId) {
								delList.add(model);
							}
						}
					}
					for (int j = 1; j <= delDay; j++) {
						List<ClubMemberRecordModel> tmpList = playerMap.get(j);
						if (tmpList != null && tmpList.size() > 1) {
							delDayMap.get(j).addAll(tmpList.subList(0, tmpList.size() - 1));
						}
					}
				}
			}
			String msg = "=====================day 0 删除记录的数量:" + delList.size() + "=====================";
			sb.append(msg).append("\n");
			System.out.println(msg);
			for (ClubMemberRecordModel model : delList) {
				String msg1 = "==== id=" + model.getId() + " clubId=" + model.getClubId() + " accountId=" + model.getAccountId() + " day="
						+ model.getDay();
				sb.append(msg1).append("\n");
				System.out.println(msg1);
			}

			for (int k = 1; k <= delDay; k++) {
				msg = "=====================day " + k + " 删除记录的数量:" + delDayMap.get(k).size() + "=====================";
				sb.append(msg).append("\n");
				System.out.println(msg);
				for (ClubMemberRecordModel model : delDayMap.get(k)) {
					String msg1 = "==== id=" + model.getId() + " clubId=" + model.getClubId() + " accountId=" + model.getAccountId() + " day="
							+ model.getDay();
					sb.append(msg1).append("\n");
					System.out.println(msg1);
				}
			}

			try {
				FileUtils.writeStringToFile(new File("record/" + extFileName), sb.toString(), Charset.defaultCharset(), false);
			} catch (IOException e) {
				e.printStackTrace();
			}

			// 删除记录
//			if (!delList.isEmpty()) {
//				SpringService.getBean(ClubDaoService.class).batchDelete("deleteClubMemberRecordById", delList);
//			}
//			for (List<ClubMemberRecordModel> list : delDayMap.values()) {
//				if (!list.isEmpty()) {
//					SpringService.getBean(ClubDaoService.class).batchDelete("deleteClubMemberRecordById", list);
//				}
//			}

			System.out.println("===============================删除完成=======================================");
			System.exit(1);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static Map<Integer, Map<Long, List<ClubMemberRecordModel>>> initAllMemberRecord() {
		Map<Integer, Map<Long, List<ClubMemberRecordModel>>> map = new HashMap<>();
		List<ClubMemberRecordModel> allmemberRecord = SpringService.getBean(ClubDaoService.class).getDao().getAllClubMemberRecord();
		for (ClubMemberRecordModel model : allmemberRecord) {
			int clubId = model.getClubId();
			long accountId = model.getAccountId();
			if (!map.containsKey(clubId)) {
				Map<Long, List<ClubMemberRecordModel>> temp = new HashMap<>();
				map.put(model.getClubId(), temp);
			}
			Map<Long, List<ClubMemberRecordModel>> map1 = map.get(clubId);
			if (!map1.containsKey(accountId)) {
				List<ClubMemberRecordModel> temp = new ArrayList<>();
				map1.put(accountId, temp);
			}
			List<ClubMemberRecordModel> list = map1.get(accountId);
			list.add(model);
		}
		return map;
	}

	@Override
	public void stop() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	protected void config(PropertiesUtil prop) {
		// TODO Auto-generated method stub

	}

	@Override
	protected Class<? extends IOEventListener<C2SSession>> acceptorListener() {
		// TODO Auto-generated method stub
		return null;
	}

}
