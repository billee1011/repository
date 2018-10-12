package com.lingyu.game;

import java.io.Serializable;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.TypeReference;
import com.lingyu.common.entity.Role;
import com.lingyu.common.io.MsgType;
import com.lingyu.common.io.Session;
import com.lingyu.msg.rpc.LoadDataAck;
import com.lingyu.msg.rpc.LoadDataReq;
import com.lingyu.msg.rpc.LoadUserDataAck;
import com.lingyu.msg.rpc.LoadUserDataReq;
import com.lingyu.msg.rpc.WriteDataReq;
import com.lingyu.noark.data.EntityMapping;
import com.lingyu.noark.data.OperateType;
import com.lingyu.noark.data.RedisEntityMapping;
import com.lingyu.noark.data.accessor.network.NetworkDataSource;

public class GameNetworkDataSource implements NetworkDataSource {
	private static final Logger logger = LogManager.getLogger(GameNetworkDataSource.class);

	private final long roleId;
	private final long userId;
	private final Session session;

	public GameNetworkDataSource(long roleId, long userId, Session session) {
		this.roleId = roleId;
		this.userId = userId;
		this.session = session;
	}

	@Override
	public Serializable getRoleId() {
		return roleId;
	}

	@Override
	public Serializable getUserId() {
		return userId;
	}

	@Override
	public <T> int writeData(EntityMapping<T> em, OperateType type, T entity) {
		WriteDataReq<T> req = new WriteDataReq<>();
		req.setKlassName(em.getEntityClass().getName());
		req.setType(type);
		req.setEntity(entity);
		session.sendRPC(MsgType.RPC_WriteDataReq, req);
		return 0;
	}

	@Override
	public <T> T loadData(EntityMapping<T> em) {
		List<T> result = this.loadDataList(em);
		return result.isEmpty() ? null : (T) result.get(0);
	}

	@Override
	public <T> List<T> loadDataList(EntityMapping<T> em) {
		LoadDataReq req = new LoadDataReq();
		req.setRoleId(roleId);
		req.setKlassName(em.getEntityClass().getName());
		LoadDataAck<T> ack = session.sendRPCWithReturn(MsgType.RPC_LoadDataReq, req, new TypeReference<LoadDataAck<T>>() {
		});
		if (Role.class.equals(em.getEntityClass())) {
			logger.info("拉到数据：{}", ack.getResult());
		}
		return ack.getResult();
	}

	@Override
	public <T> T loadData(EntityMapping<T> arg0, Serializable userId) {
		int msgType = MsgType.RPC_LoadUserDataReq;
//		if (arg0.getEntityClass() == Vip.class) {
//			msgType = MsgType.RPC_LoadVipDataReq;
//		}
		LoadUserDataReq req = new LoadUserDataReq();
		req.setUserId((Long) userId);
		LoadUserDataAck<T> ack = session.sendRPCWithReturn(msgType, req, new TypeReference<LoadUserDataAck<T>>() {
		});
		return ack.getResult();
	}

	@Override
	public <T> void writeData(OperateType type, T data, RedisEntityMapping<?> rem) {
		WriteDataReq<T> req = new WriteDataReq<>();
		req.setKlassName(rem.getEntityClass().getName());
		req.setType(type);
		req.setEntity(data);
		session.sendRPC(MsgType.RPC_WriteDataReq, req);
	}
}