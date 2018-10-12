package com.lingyu.noark.data.accessor.network;

import java.io.Serializable;
import java.util.List;

import com.lingyu.noark.data.OperateType;

/**
 * 一次数据操作的网络消息。
 * 
 * @author 小流氓<176543888@qq.com>
 */
public class DataOperateMsg {
	Serializable roleId;
	List<DataOperate> datas;

	class DataOperate {
		Class<?> entityClass;
		Object entity;
		OperateType type;
	}
}
