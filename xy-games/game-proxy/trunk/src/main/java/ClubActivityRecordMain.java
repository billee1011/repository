/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */

import java.text.SimpleDateFormat;
import java.util.Date;

import com.cai.common.util.AbstractServer;
import com.cai.common.util.PropertiesUtil;
import com.cai.common.util.TimeUtil;
import com.cai.handler.s2s.ClubActivitySnapshotRspHandler;
import com.xianyi.framework.core.transport.event.IOEventListener;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

import protobuf.clazz.ClubMsgProto;

/**
 * 生成俱乐部大赢家次数
 *
 * @author wu_hc date: 2018年5月4日 下午3:39:20 <br/>
 */
public final class ClubActivityRecordMain extends AbstractServer {

	static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public static void main(String[] args) {
		try {

			ClubActivitySnapshotRspHandler handler = new ClubActivitySnapshotRspHandler();
			ClubMsgProto.ClubActivityProto.Builder builder = ClubMsgProto.ClubActivityProto.newBuilder();
			builder.setClubId(3188768);
			builder.setActivityType(1);
			Date start = TimeUtil.getParsedDate("2018-08-24 08:20:00");
			Date end = TimeUtil.getParsedDate("2018-08-25 02:30:00");
			builder.setStartDate((int) (start.getTime() / 1000L));
			builder.setEndDate((int) (end.getTime() / 1000L));
			builder.setActivityId(8328);
			builder.setCreatorId(687683);
			builder.setActivityName("俱乐部有奖活动");
			System.out.println(builder.build());
			Runnable task = handler.newTask(builder.build());
			task.run();

			System.exit(1);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public void stop() throws Exception {
	}

	@Override
	protected void config(PropertiesUtil prop) {

	}

	@Override
	protected Class<? extends IOEventListener<C2SSession>> acceptorListener() {
		return null;
	}

	@Override
	protected void debugCmdAccept(String cmd) {
	}

}
