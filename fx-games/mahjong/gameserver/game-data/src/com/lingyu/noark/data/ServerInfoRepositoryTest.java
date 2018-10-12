package com.lingyu.noark.data;

import org.junit.Test;

import com.lingyu.noark.data.entity.ServerInfo;
import com.lingyu.noark.data.repository.ServerInfoRepository;

public class ServerInfoRepositoryTest extends AbstractRepositoryTest {
	private ServerInfoRepository serverInfoRepository = new ServerInfoRepository();

	@Test
	public void testInsterAndUpdate() {
		ServerInfo info = serverInfoRepository.cacheLoad(10086);
		if (info == null) {
			info = new ServerInfo();
			info.setId(10086);
			info.setName("第10086个新服-抢占日本岛");
			serverInfoRepository.cacheInsert(info);
		} else {
			info.setName("第10010个新服-攻占太平洋");
			serverInfoRepository.cacheUpdate(info);
		}
	}

	@Test
	public void testSelete() {
		ServerInfo info = serverInfoRepository.cacheLoad(10086);
		System.out.println("Info:" + info);
	}

	@Test
	public void testDelete() {
		serverInfoRepository.cacheDelete(10086);
	}
}
