package com.cai.handler.client;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import com.cai.common.constant.C2SCmd;
import com.cai.common.domain.CityHoursLogModel;
import com.cai.common.domain.CityLogModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.util.MyDateUtil;
import com.cai.constant.CityReportModel;
import com.cai.dictionary.CityDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.service.MongoDBServiceImpl;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import org.apache.commons.lang.StringUtils;
import protobuf.clazz.c2s.C2SProto.CityReport;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

@ICmd(code = C2SCmd.CITY_REPORT, desc = "客户端城市上报")
public class CityReportHandler extends IClientExHandler<CityReport> {

	private static CityReportModel cityReportModel = new CityReportModel();

	@Override
	protected void execute(CityReport req, TransmitProto topReq, C2SSession session) throws Exception {
		long accountId = topReq.getAccountId();
		String cityCode = req.getCityCode();
		if (StringUtils.isBlank(cityCode)) {
			return;
		}
		int code = Integer.parseInt(cityCode);
		cityReportModel.addCityReport(accountId, code);
	}

	public static void taskInDb() {
		SysParamModel sysParamModel2237 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2237);
		if (sysParamModel2237 != null && sysParamModel2237.getVal3() == 1) {
			return;
		}
		Map<Integer, Set<Long>> map = cityReportModel.getCityMap();
		MongoDBServiceImpl mongoDBServiceImpl = MongoDBServiceImpl.getInstance();
		Date date = MyDateUtil.getYesterdayZeroDate(new Date().getTime());
		for (int key : map.keySet()) {
			CityLogModel model = new CityLogModel();
			model.setCity_code(key);
			model.setCity_name(CityDict.getInstance().getCityNameByCode(key));
			model.setPerson_counts(map.get(key).size());
			model.setCreate_time(date);
			mongoDBServiceImpl.getLogQueue().add(model);
		}
	}

	public static void taskInDbByHours() {
		SysParamModel sysParamModel2237 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2237);
		if (sysParamModel2237 != null && sysParamModel2237.getVal2() == 1) {
			return;
		}
		Map<Integer, Set<Long>> map = cityReportModel.getCityMap();
		MongoDBServiceImpl mongoDBServiceImpl = MongoDBServiceImpl.getInstance();
		Date date = new Date();
		for (int key : map.keySet()) {
			CityHoursLogModel model = new CityHoursLogModel();
			model.setCity_code(key);
			model.setCity_name(CityDict.getInstance().getCityNameByCode(key));
			model.setPerson_counts(map.get(key).size());
			model.setCreate_time(date);
			mongoDBServiceImpl.getLogQueue().add(model);
		}
	}

	public static void clearCityReportModel() {
		cityReportModel.clearMap();
	}
}
