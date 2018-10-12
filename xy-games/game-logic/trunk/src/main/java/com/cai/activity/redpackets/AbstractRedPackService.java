package com.cai.activity.redpackets;

import java.util.Date;
import java.util.List;
import com.cai.common.domain.RedPackageActivityModel;
import com.cai.dictionary.RedPackageRuleDict;
import com.cai.util.TimeUtil;
import com.cai.util.Tuple;
import com.google.common.collect.Lists;


public abstract class AbstractRedPackService implements IRedPackService {

	@Override
	public boolean isStart() {
		List<Tuple<Date, Date>> list = activityTime();
		for (Tuple<Date, Date> t : list) {
			Date startTime = t.getLeft();
			Date endTime = t.getRight();
			Date curr = new Date();

			if (curr.after(startTime) && endTime.after(curr))
				return true;
		}
		return false;
	}

	public List<Tuple<Date, Date>> activityTime() {
		List<Tuple<Date, Date>> timeList = Lists.newArrayList();

		RedPackageActivityModel dataModel = RedPackageRuleDict.getInstance().getRedPackageRuleMap().get(getRedPackageActivitType());
		if(dataModel == null){
			return timeList;
		}

		String[] timeStr = dataModel.getActivity_time().split("\\|");
		for (String str : timeStr) {
			String endStr = str.split("\\~")[1];
			String startStr = str.split("\\~")[0];

			Date start = TimeUtil.parse(startStr, TimeUtil.PATTERN_yyyy_MM_dd_HH_mm_ss);
			Date end = TimeUtil.parse(endStr, TimeUtil.PATTERN_yyyy_MM_dd_HH_mm_ss);

			timeList.add(new Tuple<Date, Date>(start, end));
		}
		return timeList;
	}
	
	
}
