//package com.lingyu.noark.data.accessor.network;
//
//import java.io.Serializable;
//import java.util.Collections;
//import java.util.List;
//
//import com.lingyu.noark.data.EntityMapping;
//
///**
// * 一个简单的实现，后面在跨服功能中进来一个New一个，并注册到网络管理器中.
// * 
// * @see com.lingyu.noark.data.DataManager#register(NetworkAccessor)
// */
//public final class SimpleNetworkAccessor implements NetworkAccessor {
//
//	private final Long roleId;
//	private final Session session;
//
//	public SimpleNetworkAccessor(Long roleId, Session session) {
//		this.roleId = roleId;
//		this.session = session;
//	}
//
//	@Override
//	public Serializable getRoleId() {
//		return roleId;
//	}
//
//	@Override
//	public List<Object> loadData(EntityMapping em) {
//		// session.write(); session.read(byte[]);
//		// 最好实现一个RPC调用
//		return Collections.emptyList();
//	}
//
//	@Override
//	public int writeData(EntityMapping em, OperateType type, Object entity) {
//		session.write();
//		// 最好实现一个RPC调用
//		return 1;
//	}
//
//	/**
//	 * 虚拟的Session.
//	 * <p>
//	 * 实际上可能是由其他开源框架来实现.比如Netty和Mina
//	 */
//	class Session {
//		public void write() {
//		}
//	}
// }
