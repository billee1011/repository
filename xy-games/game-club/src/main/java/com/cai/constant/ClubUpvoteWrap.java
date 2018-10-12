/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.constant;

/**
 * 
 * 
 * 俱乐部点赞
 * 
 * @author wu_hc date: 2017年11月22日 上午11:15:45 <br/>
 */
public final class ClubUpvoteWrap {

//	/**
//	 * 存活时间
//	 */
//	private static final int ALIVE = 8 * 24 * 60 * 60;
//
//	/**
//	 * 点赞消息
//	 */
//	private final Map<Long, CommonLII> upvote = Maps.newConcurrentMap();
//
//	/**
//	 * 俱乐部id
//	 */
//	private final int clubId;
//
//	/**
//	 * @param clubId
//	 * @param source
//	 */
//	public ClubUpvoteWrap(int clubId, byte[] source) {
//		this.clubId = clubId;
//		this.decode(source);
//	}
//
//	public int getClubId() {
//		return clubId;
//	}
//
//	public void upvote(List<CommonLII> votes) {
//		votes.forEach((vote) -> {
//			upvote.put(vote.getK(), vote);
//		});
//	}
//
//	/**
//	 * 序列化
//	 * 
//	 * @return
//	 */
//	public ClubUpvoteProto.Builder encode() {
//		ClubUpvoteProto.Builder builder = ClubUpvoteProto.newBuilder();
//		builder.setClubId(clubId);
//		upvote.forEach((id, vote) -> {
//			if ((int) (System.currentTimeMillis() / 1000) - vote.getV2() < ALIVE) {
//				builder.addCommon(vote);
//			}
//		});
//		return builder;
//	}
//
//	/**
//	 * 反序列化
//	 * 
//	 * @param source
//	 */
//	public void decode(final byte[] source) {
//		try {
//			ClubUpvoteProto proto = ClubUpvoteProto.parseFrom(source);
//			proto.getCommonList().forEach((model) -> {
//				upvote.put(model.getK(), model);
//			});
//		} catch (InvalidProtocolBufferException e) {
//			e.printStackTrace();
//		}
//	}

}
