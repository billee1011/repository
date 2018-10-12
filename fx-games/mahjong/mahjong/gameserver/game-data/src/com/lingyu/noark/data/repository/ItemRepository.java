package com.lingyu.noark.data.repository;

import java.util.List;

import com.lingyu.noark.data.entity.Item;

//@Repository
public class ItemRepository extends MultiCacheRepository<Item, Long> {

	/**
	 * 获取背包内的道具.
	 */
	public List<Item> getItemInBag(long roleId) {
		return this.cacheLoadAll(roleId, new QueryFilter<Item>() {
			@Override
			public boolean check(Item t) {
				return true;// t.getTemplateId() == 123456;
			}

			@Override
			public boolean stopped() {
				return false;
			}
		});
	}
}
