package com.lingyu.admin;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lingyu.admin.manager.GameAreaManager;
import com.lingyu.common.core.ErrorCode;
import com.lingyu.common.entity.GameArea;
import com.lingyu.common.io.MsgType;
import com.lingyu.common.io.Session;
import com.lingyu.common.io.SessionManager;
import com.lingyu.common.util.ConvertObjectUtil;

@Service
public class AdminProcessorImpl extends AdminProcessor {
	private static final Logger logger = LogManager.getLogger(AdminProcessorImpl.class);
	@Autowired
	private GameAreaManager gameAreaManager;

	@Override
	public void keepAlive(Session session, Object[] msg) {
		session.sendMsg(new Object[] { MsgType.Ping_S2C_Msg, new Object[] {} });
	}

	@Override
	public void login(Session session, Object[] msg) {
		String pid = (String) msg[0];
		int userId = (int) msg[1];
		Session old = SessionManager.getInstance().getSession4User(userId);
		if (old != null) {
			logger.warn("用户被挤下线 userId={},channel={},id={}",userId,old.getChannel(),old.getId());
			old.sendMsg(new Object[] { MsgType.Disconnect_S2C_Msg, new Object[] {} });
			old.close();
//			//为了避免
//			Thread.sleep(5000);
		}
		logger.info("用户登录 userId={},channel={},id={}",userId,session.getChannel(),session.getId());
		SessionManager.getInstance().addSession4User(pid, userId, session);
		session.sendMsg(new Object[] { MsgType.Login_S2C_Msg, new Object[] {} });
	}

	@Override
	public void getServerList(Session session, Object[] msg) {
		List<GameArea> list = gameAreaManager.getGameAreaList(session.getPid());
		List<Object[]> areaList = new ArrayList<>();
		Date now = new Date();
		for (GameArea e : list) {
			if (e.getAddTime().before(now)) {
				areaList.add(e.toAreaVO());
			}
		}
		session.sendMsg(new Object[] { MsgType.GetServerList_S2C_Msg, areaList.toArray() });
	}

	@Deprecated
	@Override
	public void selectArea(Session session, Object[] msg) {
		String pid = (String) msg[0];
		int areaId = (int) msg[1];
		logger.info("pid={},areaId={}", pid, areaId);
		session.sendMsg(new Object[] { MsgType.SelectArea_S2C_Msg, new Object[] {} });
	}

	@Deprecated
	@Override
	public void deselectArea(Session session, Object[] msg) {
		String pid = (String) msg[0];
		int areaId = (int) msg[1];
		logger.info("pid={},areaId={}", pid, areaId);
		// chatMonitorManager.remove4ObserverList(pid, areaId,
		// session.getUserId());
		session.sendMsg(new Object[] { MsgType.DeselectArea_S2C_Msg, new Object[] {} });
	}

	@Override
	public void punish(Session session, Object[] msg) {
		String pid = (String) msg[0];
		int areaId = (int) msg[1];
		int type = (int) msg[2];
		String name = (String) msg[3];
		Date beginTime = new Date(ConvertObjectUtil.arg2long(msg[4]));
		Date endTime = new Date(ConvertObjectUtil.arg2long(msg[5]));
		String reason = (String) msg[6];
		long roleId = ConvertObjectUtil.arg2long(msg[7]);
		logger.info("pid={},areaId={},type={},name={},beginTime={},endTime={},reason={}", pid, areaId, type, name, beginTime, endTime, reason);
		session.sendMsg(new Object[] { MsgType.Ping_S2C_Msg, new Object[] { ErrorCode.EC_OK } });
	}

	// http://xxxxx.swf?pid=1212&area_id=2&user_id=2132&user_name=whh&ip=192.168.1.21&port=8888

	@Override
	public void chat(Session session, Object[] msg) {
		String pid = (String) msg[0];
		int worldId = (int) msg[1];
		int areaId = (int) msg[2];
		int channelType = (int) msg[3];
		String content = (String) msg[4];
		//if (channelType == ChannelConstant.CHANNEL_TYPE_PRIVATE) {
			long roleId = ConvertObjectUtil.arg2long(msg[5]);
			String roleName = (String) msg[6];
			logger.info("userId={},userName={},areaId={},content={},roleId={},roleName={}", session.getUserId(), session.getUserName(), areaId, content, roleId,
					roleName);
	//	}

	}

	/** 获取玩家问的问题列表 */
	@Override
	public void getQuestionList(Session session, Object[] msg) {
		/*Collection<TalkDTO> list = chatMonitorManager.getQuestionlist();
		Object[] result = new Object[list.size()];
		int i = 0;
		for (TalkDTO e : list) {
			result[i++] = e.toVO();
		}
		session.sendMsg(new Object[] { MsgType.GET_QUESTION_LIST_S2C_Msg, result });*/

	}

	/** 接客 */
	@Override
	public void receptionPlayer(Session session, Object[] msg) {
		int id = (int) msg[0];
		// TalkDTO dto = chatMonitorManager.remove(id);
		// if (dto != null) {
		// session.sendMsg(new Object[] { MsgType.Reception_Player_S2C_Msg, new
		// Object[] { ErrorCode.EC_OK, dto.toVO() } });
		// } else {
		// session.sendMsg(new Object[] { MsgType.Reception_Player_S2C_Msg, new
		// Object[] { ErrorCode.EC_FAILED } });
		// }
	}
}
