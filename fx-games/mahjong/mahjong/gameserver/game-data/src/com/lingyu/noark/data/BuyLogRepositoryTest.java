package com.lingyu.noark.data;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import org.junit.Test;

import com.lingyu.noark.data.entity.Attribute;
import com.lingyu.noark.data.entity.BuyLog;
import com.lingyu.noark.data.repository.BuyLogRepository;

public class BuyLogRepositoryTest {
	private BuyLogRepository buyLogRepository = new BuyLogRepository();

	@Test
	public void test() {
		new DataManager(false,1, 2,true);
		BuyLog item = new BuyLog();
		item.setId(1234567890);
		item.setName("伙胡道具");
		item.setTemplateId(10010);
		item.setRoleId(9876543210L);
		item.setBind(true);
		item.setAddTime(new Date());
		item.setAttribute(new Attribute(1));
		System.out.println(buyLogRepository.logInsert(item));
		System.out.println(buyLogRepository.logUpdate(item));
		System.out.println(buyLogRepository.logDelete(item));
		System.out.println(buyLogRepository.logNInsert(item));
	}

	@Test
	public void testDate() {
		Date now = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HH");
		System.out.println(sdf.format(now));

		String tableName = "buy_log_";
		String[] tns = tableName.split("[{|}]");
		System.out.println(Arrays.toString(tns));
	}
}