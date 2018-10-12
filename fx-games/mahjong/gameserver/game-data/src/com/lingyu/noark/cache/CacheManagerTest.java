package com.lingyu.noark.cache;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class CacheManagerTest {

	private CacheManager cacheManager = new SimpleCustomCacheManager();
	private final String cacheKey = "Test";

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void test() throws InterruptedException {
		cacheManager.putData(cacheKey, 1, 1);
		Thread.sleep(4000);

		// cacheManager.putData(cacheKey, 2, 2);
		// System.out.println(cacheManager.getData(cacheKey, (long) 2));
		// cacheManager.removeData(cacheKey, 2);
		// System.out.println(cacheManager.getData(cacheKey, (int) 2));
	}
}
