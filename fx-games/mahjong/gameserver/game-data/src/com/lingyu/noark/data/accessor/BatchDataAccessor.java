package com.lingyu.noark.data.accessor;

import java.io.Serializable;
import java.util.List;

import com.lingyu.noark.data.EntityMapping;
import com.lingyu.noark.data.write.impl.EntityOperate;

/**
 * 批量数据访问接口.
 * 
 * @author 小流氓<176543888@qq.com>
 */
public interface BatchDataAccessor {

	/**
	 * 加载指定角色Id对应模块数据.
	 * 
	 * @param roleId 角色Id
	 * @param em 对象实体描述类.
	 * @return 返回这个角色Id的模块数据.
	 */
	public List<List<?>> loadByRoleId(Serializable roleId, List<EntityMapping<?>> em);

	/**
	 * 写入角色的修改过的数据.
	 * <p>
	 * 如果是Mysql需要合并分类实现批量操作，Network要实现一次发送.
	 * 
	 * @param roleId 角色Id
	 * @param entitys 修改过的数据.
	 */
	public void writeByRoleId(Serializable roleId, List<EntityOperate<?>> entitys);
}
