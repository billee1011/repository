package com.lingyu.noark.data;

import org.junit.Test;

import com.lingyu.noark.data.entity.Horse;
import com.lingyu.noark.data.repository.HorseRepository;

public class HorseRepositoryTest extends AbstractRepositoryTest {
	private HorseRepository horseRepository = new HorseRepository();

	@Test
	public void testInsterAndUpdate() {
		Horse info = horseRepository.cacheLoad(123L);
		if (info == null) {
			info = new Horse();
			info.setId(123);
			info.setName("坐骑");
			horseRepository.cacheInsert(info);
		} else {
			info.setName("坐骑2");
			horseRepository.cacheUpdate(info);
		}
	}

	@Test
	public void testSelete() {
		Horse info = horseRepository.cacheLoad(123L);
		System.out.println("Info:" + info);
	}

	@Test
	public void testDelete() {
		horseRepository.cacheDelete(123L);
	}
}
