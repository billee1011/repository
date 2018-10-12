/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.constant;

import java.util.Date;

import com.cai.common.domain.ClubBulletinModel;
import com.cai.common.util.Lifecycle;

import protobuf.clazz.ClubMsgProto.ClubBulletinProto;

/**
 * 公告包装
 * 
 *
 * @author wu_hc date: 2018年4月12日 下午3:19:12 <br/>
 */
public final class ClubBulletinWrap implements Lifecycle {

	/**
	 * 日志
	 */
	// private static final Logger logger =
	// LoggerFactory.getLogger(ClubBulletinWrap.class);

	/**
	 */
	private final ClubBulletinModel bulletinModel;

	public ClubBulletinWrap(ClubBulletinModel bulletinModel) {
		this.bulletinModel = bulletinModel;
	}

	@Override
	public void start() throws Exception {
	}

	@Override
	public void stop() throws Exception {
	}

	@Override
	public boolean isRunning() {
		return true;
	}

	public ClubBulletinModel getBulletinModel() {
		return bulletinModel;
	}

	public long getId() {
		return bulletinModel.getId();
	}

	/**
	 * 
	 * @return
	 */
	public final ClubBulletinProto.Builder toBuilder() {
		ClubBulletinProto.Builder b = ClubBulletinProto.newBuilder().setId(bulletinModel.getId()).setClubId(bulletinModel.getClubId())
				.setCreator(bulletinModel.getCreatorId()).setText(bulletinModel.getText()).setCategory(bulletinModel.getCategory());
		b.setStatus(bulletinModel.getStatus());
		Date startDate = bulletinModel.getStartDate();
		if (null != startDate) {
			b.setStartDate((int) (startDate.getTime() / 1000L));
		}

		Date endDate = bulletinModel.getEndDate();
		if (null != endDate) {
			b.setEndDate((int) (endDate.getTime() / 1000L));
		}
		return b;
	}

	public boolean isDone(long cur) {
		return cur > bulletinModel.getEndDate().getTime();
	}
}
