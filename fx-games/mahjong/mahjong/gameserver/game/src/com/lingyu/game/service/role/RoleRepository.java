package com.lingyu.game.service.role;

import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Repository;
import com.lingyu.common.entity.Role;
import com.lingyu.noark.data.repository.QueryFilter;
import com.lingyu.noark.data.repository.UniqueCacheRepository;

@Repository
public class RoleRepository extends UniqueCacheRepository<Role, Long> {

	@Override
	public void cacheUpdate(Role entity) {
		entity.setModifyTime(new Date());
		super.cacheUpdate(entity);
	}
	
	public List<Role> getRoleListByUserId(final String pid, final String userId) {
		return this.cacheLoadAll(new QueryFilter<Role>() {
			@Override
			public boolean stopped() {
				return false;
			}

			@Override
			public boolean check(Role role) {
				boolean flag = role.getUserId().equals(userId);
				if (flag && pid.equals(role.getPid())) {// 如果账号一致，判定一下这个角色是不是来源网络层
					return true;
				}
				return false;
			}
		});
	}
}