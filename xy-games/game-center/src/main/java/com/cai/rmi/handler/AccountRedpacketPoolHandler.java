package com.cai.rmi.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RMICmd;
import com.cai.common.define.ERedpacketPoolType;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.rmi.vo.RedpacketPoolRMIVo;
import com.cai.service.AccountRedpacketPoolService;

/**
 * 
 * 新版闲逸助手返利相关操作
 *
 * @author tang date: 2018年03月23日 上午10:43:45 <br/>
 */
@IRmi(cmd = RMICmd.REDPACKET_POOL, desc = "红包池相关操作")
public final class AccountRedpacketPoolHandler extends IRMIHandler<RedpacketPoolRMIVo, Integer> {
	private static Logger logger = LoggerFactory.getLogger(AccountRedpacketPoolHandler.class);

	private static int SUCCESS = 1;
	private static int FAIL = 0;
	@Override
	protected Integer execute(RedpacketPoolRMIVo vo) {
		boolean res = false;
		try{
			if(vo.getType()==ERedpacketPoolType.PUT.getId()){
				res = AccountRedpacketPoolService.getInstance().addMoney(vo.getAccountId(), vo.getMoney());
				return res?SUCCESS:FAIL;
			}else if(vo.getType()==ERedpacketPoolType.TAKE.getId()){
				res = AccountRedpacketPoolService.getInstance().takeMoney(vo.getAccountId(), vo.getMoney(),vo.getType(),"领取红包");
				return res?SUCCESS:FAIL;
			}else if(vo.getType()==ERedpacketPoolType.EXCHANGE.getId()){
				res = AccountRedpacketPoolService.getInstance().takeMoney(vo.getAccountId(), vo.getMoney(),vo.getType(),"兑换闲逸豆");
				return res?SUCCESS:FAIL;
			}else if(vo.getType()==ERedpacketPoolType.CLEAR.getId()){
				AccountRedpacketPoolService.getInstance().clearRedpacketPool();
				//暂时未加
			}else if(vo.getType()==ERedpacketPoolType.DETAIL.getId()){
				int money = AccountRedpacketPoolService.getInstance().getAccountRedpacketPoolModel(vo.getAccountId());
				return money;
			}
		}catch(Exception e){
			logger.error(vo.toString()+" accountRedpacketPoolHandler error",e);
		}
		return FAIL;
	}

}
