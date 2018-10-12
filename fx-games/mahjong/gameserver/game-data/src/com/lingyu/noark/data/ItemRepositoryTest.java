package com.lingyu.noark.data;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.lingyu.noark.data.accessor.AnnotationEntityMaker;
import com.lingyu.noark.data.accessor.Page;
import com.lingyu.noark.data.accessor.Pageable;
import com.lingyu.noark.data.accessor.Sort;
import com.lingyu.noark.data.accessor.Sort.Direction;
import com.lingyu.noark.data.entity.Attribute;
import com.lingyu.noark.data.entity.Item;
import com.lingyu.noark.data.repository.ItemRepository;

public class ItemRepositoryTest extends AbstractRepositoryTest {

	private ItemRepository itemRepository = new ItemRepository();

	private long roleId = 123456;
	// 源
	private Item item = new Item();
	{
		item.setId(1234567890);
		item.setName("定时存档的时间间隔为 30秒, 离线玩家在内存中的存活时间为 300秒");
		item.setTemplateId(10010);
		item.setRoleId(9876543210L);
		item.setBind(true);
		item.setAddTime(new Date());
		item.setAttribute(new Attribute(1));
	}

	@Test
	public void test1() throws CloneNotSupportedException, InstantiationException, IllegalAccessException {
		EntityMapping<Item> em = new AnnotationEntityMaker().make(Item.class);
		for (int j = 0; j < 1; j++) {
			long start = System.nanoTime();
			for (int i = 0; i < 1000; i++) {
				// item.clone();
				em.clone(item);
			}
			System.out.println((System.nanoTime() - start) / 1000000f);
		}

	}

	@Test
	public void testRandom() {
		Random r = new Random();
		while (true) {
			long roleId = r.nextLong() % 3000;
			long itemId = r.nextLong() % 200;

			switch (r.nextInt(3)) {
			case 0: {
				// 尝试插入或修改
				Item item = itemRepository.cacheLoad(roleId, itemId);
				if (item == null) {
					item = new Item();
					item.setId(itemId);
					item.setName("XXX");
					item.setTemplateId(10010);
					item.setRoleId(roleId);
					item.setAddTime(new Date());
					item.setBind(true);
					item.setAttribute(new Attribute(1));
					itemRepository.cacheInsert(item);
				} else {
					item.setName("" + System.currentTimeMillis());
					itemRepository.cacheUpdate(item);
				}
			}
				break;
			case 1: {
				Item item = itemRepository.cacheLoad(roleId, itemId);
				if (item != null) {
					itemRepository.delete(item);
				}
			}
				break;
			case 2: {
				this.testSelectAll();
				break;
			}
			}
		}

		// for (long i = 1; i < 100; i++) {
		// switch (r.nextInt(4)) {
		// case 0: {
		// Item item = itemRepository.cacheLoad(roleId, i);
		// if (item == null) {
		// item = new Item();
		// item.setId(i);
		// item.setName("XXX");
		// item.setTemplateId(10010);
		// item.setRoleId(roleId);
		// item.setBind(true);
		// item.setAddTime(new Date());
		// item.setAttribute(new Attribute(1));
		// itemRepository.cacheInster(item);
		// } else {
		// item.setName("" + System.currentTimeMillis());
		// itemRepository.cacheUpdate(item);
		// }
		// }
		// break;
		// case 1: {
		// this.testSelectAll();
		// }
		// break;
		// case 2: {
		// Item item = itemRepository.cacheLoad(roleId, i);
		// if (item != null) {
		// itemRepository.cacheDelete(roleId, i);
		// }
		// }
		// break;
		// case 3: {
		// Item item = itemRepository.cacheLoad(roleId, i);
		// if (item != null) {
		// itemRepository.cacheDelete(roleId, i);
		// }
		// }
		// break;
		// }
		// }
	}

	@Test
	public void testInsterAll() {
		for (long itemId = 10000; itemId < 10030; itemId++) {
			Item item = itemRepository.cacheLoad(roleId, itemId);
			if (item == null) {
				item = new Item();
				item.setId(itemId);
				item.setName("XXX");
				item.setTemplateId(10010);
				item.setRoleId(roleId);
				item.setBind(true);
				item.setAddTime(new Date());
				item.setAttribute(new Attribute(1));
				itemRepository.cacheInsert(item);
			}
		}
	}

	@Test
	public void testInster() {
		Item item = new Item();
		item.setId(122);
		item.setName("XXX");
		item.setTemplateId(10010);
		item.setRoleId(roleId);
		item.setIntx(new AtomicInteger(0));
		// item.setAddTime(new Date());
		item.setAttribute(new Attribute(1));
		itemRepository.cacheInsert(item);
	}

	@Test
	public void testUpdate() {
		Item item = itemRepository.cacheLoad(roleId, 122L);
		item.setName("xxx");
		item.setAddTime(new Date());
		item.setTemplateId(123456);
		item.setAttribute(new Attribute(123));
		item.setMoney(1.02123123123f);
		item.setMoney1(12.7D);
		item.getIntx().incrementAndGet();
		itemRepository.cacheUpdate(item);
	}

	@Test
	public void testDelete() {
		Item item = itemRepository.cacheLoad(roleId, 1L);
		itemRepository.delete(item);
		// itemRepository.cacheDeleteAll(roleId);
	}

	@Test
	public void testSelectAll() {
		List<Item> items = itemRepository.getItemInBag(roleId);
		System.out.println(Arrays.toString(items.toArray()));
	}

	@Test
	public void testSql() {
		List<Item> items = itemRepository.queryForList("select * from item where role_id=? and template_id=?", roleId, 123456);
		System.out.println(Arrays.toString(items.toArray()));
	}

	@Test
	public void testSq111l() {
		long count = itemRepository.queryForLong("select count(1) from item");
		System.out.println("count===>" + count);
	}

	@Test
	public void testSq11123131l() {
		Map<String, Object> xx = itemRepository.queryForMap("select * from item");
		System.out.println("map===>" + xx);
	}

	@Test
	public void testSqlPage() {
		Page<Item> xx = itemRepository.loadAllBySystem(new Pageable(2, 11, new Sort(Direction.DESC, "id")));
		System.out.println("page===>" + xx);
	}
}
