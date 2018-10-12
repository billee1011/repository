package com.cai.rmi.handler;

import com.cai.common.constant.RMICmd;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.util.IDGeneratorOrder;

/**
 * 
 *
 * @author zhanglong date: 2018年4月20日 上午10:57:45
 */
@IRmi(cmd = RMICmd.GEN_USE_RED_PACKET_ORDER_ID, desc = "生成使用红包订单号")
public final class GenUseRedPacketOrderId extends IRMIHandler<Long, String> {

	@Override
	protected String execute(Long accountId) {
		return IDGeneratorOrder.getInstance().getUseRedPacketUniqueID();
	}
}
