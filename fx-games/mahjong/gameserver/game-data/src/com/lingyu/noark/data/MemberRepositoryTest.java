package com.lingyu.noark.data;

import org.junit.Test;

import com.lingyu.noark.data.entity.Member;
import com.lingyu.noark.data.repository.MemberRepository;

public class MemberRepositoryTest extends AbstractRepositoryTest {
	private MemberRepository memberRepository = new MemberRepository();

	@Test
	public void testInsterAndUpdate() {
		Member info = memberRepository.cacheLoad(1L, 1L);
		if (info == null) {
			info = new Member();
			info.setRoleId(1);
			info.setId(1);
			info.setName("坐骑");
			memberRepository.cacheInsert(info);
		} else {
			info.setName("坐骑2");
			memberRepository.cacheUpdate(info);
		}
	}

	@Test
	public void testSelete() {
		Member info = memberRepository.cacheLoad(1L, 1L);
		System.out.println("Info:" + info);
	}

	@Test
	public void testDelete() {
		Member info = memberRepository.cacheLoad(1L, 1L);
		memberRepository.cacheDelete(info);
	}
}
