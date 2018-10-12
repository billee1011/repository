/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.client;

import java.util.Date;

import com.cai.common.constant.C2SCmd;
import com.cai.common.define.ESysMsgType;
import com.cai.common.domain.ClubBulletinModel;
import com.cai.common.util.Bits;
import com.cai.common.util.EmojiFilter;
import com.cai.common.util.SpringService;
import com.cai.common.util.TimeUtil;
import com.cai.constant.Club;
import com.cai.constant.ClubBulletinCode;
import com.cai.constant.ClubBulletinWrap;
import com.cai.dictionary.DirtyWordDict;
import com.cai.service.ClubDaoService;
import com.cai.service.ClubService;
import com.cai.utils.Utils;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubBulletinProto;
import protobuf.clazz.ClubMsgProto.ClubSetBulletinProto;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 * 
 *
 * @author wu_hc date: 2018年4月12日 下午2:56:55 <br/>
 */
@ICmd(code = C2SCmd.CLUB_SET_BULLETIN, desc = "俱乐部设置公告")
public final class ClubSetBulletinHandler extends IClientExHandler<ClubSetBulletinProto> {

	@Override
	protected void execute(ClubSetBulletinProto req, TransmitProto topReq, C2SSession session) throws Exception {

		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}
		final ClubBulletinProto bulletin = req.getBulletin();

		if (null == bulletin) {
			return;
		}

		if (DirtyWordDict.getInstance().checkDirtyWord(bulletin.getText()) || EmojiFilter.containsEmoji(bulletin.getText())) {
			Utils.sendTip(topReq.getAccountId(), "公告内容非法!", ESysMsgType.NONE, session);
			return;
		}

		club.runInReqLoop(() -> {

			ClubBulletinWrap wrap = null;

			if (req.getType() == ClubBulletinCode.CREATE) {
				ClubBulletinModel model = new ClubBulletinModel();
				model.setCategory(Bits.byte_2);
				model.setClubId(club.getClubId());
				model.setCreatorId(topReq.getAccountId());
				if (bulletin.hasStartDate()) {
					model.setStartDate(new Date(bulletin.getStartDate() * 1000L));
				} else {
					model.setStartDate(new Date());
				}
				if (bulletin.hasEndDate()) {
					model.setEndDate(new Date(bulletin.getEndDate() * 1000L));
				} else {
					model.setEndDate(new Date(System.currentTimeMillis() + TimeUtil.DAY * 7));
				}
				model.setStatus(Bits.byte_1);
				model.setText(req.getBulletin().getText());

				SpringService.getBean(ClubDaoService.class).getDao().insertClubBulletin(model);
				wrap = new ClubBulletinWrap(model);
				club.bulletins.put(model.getId(), wrap);
			} else if (req.getType() == ClubBulletinCode.UPDATE) {

				wrap = club.bulletins.get(bulletin.getId());
				wrap.getBulletinModel().setText(bulletin.getText());
				wrap.getBulletinModel().setStatus((byte) bulletin.getStatus());

			} else if (req.getType() == ClubBulletinCode.START) {
				wrap = club.bulletins.get(bulletin.getId());
				wrap.getBulletinModel().setStatus(Bits.byte_1);
			} else if (req.getType() == ClubBulletinCode.STOP) {
				wrap = club.bulletins.get(bulletin.getId());
				wrap.getBulletinModel().setStatus(Bits.byte_0);
			}

			// session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(),
			// S2CCmd.CLUB_BULLETIN_INFO,
			// ClubBulletinInfoRspProto.newBuilder().setClubId(req.getClubId()).addBulletins(wrap.toBuilder())));

			Utils.bulletinEvent(topReq.getAccountId(), club, wrap.getId(), req.getType());
		});
	}
}
