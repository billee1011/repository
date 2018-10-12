package com.lingyu.noark.data;

import java.util.Date;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.lingyu.noark.data.entity.Attribute;
import com.lingyu.noark.data.entity.Item;

public class ToStringBuilderTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

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
	public void test() {
		System.out.println(ToStringBuilder.reflectionToString(item));
	}
}