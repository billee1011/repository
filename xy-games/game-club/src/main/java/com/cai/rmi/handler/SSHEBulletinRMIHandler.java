package com.cai.rmi.handler;

import static java.util.stream.Collectors.groupingBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.cai.common.constant.RMICmd;
import com.cai.common.domain.ClubBulletinModel;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.rmi.vo.SSHEBulletinVO;
import com.cai.common.util.Bits;
import com.cai.common.util.SpringService;
import com.cai.constant.Club;
import com.cai.constant.ClubBulletinCode;
import com.cai.constant.ClubBulletinWrap;
import com.cai.service.ClubDaoService;
import com.cai.service.ClubService;
import com.cai.utils.Utils;

/**
 * 
 * 
 *
 * @author wu_hc date: 2018年4月12日 下午12:32:15 <br/>
 */
@IRmi(cmd = RMICmd.CLUB_BULLET, desc = "俱乐部公告")
public final class SSHEBulletinRMIHandler extends IRMIHandler<List<SSHEBulletinVO>, Boolean> {
	@Override
	protected Boolean execute(List<SSHEBulletinVO> models) {
		boolean isSuccess = false;
		for (SSHEBulletinVO model : models) {
			ClubBulletinModel bulletinModel = model.getBulletin();
			if (bulletinModel.getCategory() != Bits.byte_1) {
				continue;
			}

			int opType = ClubBulletinCode.CREATE;
			if (bulletinModel.getId() > 0) {
				opType = ClubBulletinCode.UPDATE;
			}
			final int type = opType;
			if (model.getCategory() == SSHEBulletinVO.CATEGORY_ALL) {
				if (type == ClubBulletinCode.CREATE) {
					SpringService.getBean(ClubDaoService.class).getDao().insertClubBulletin(bulletinModel);
				} else if (type == ClubBulletinCode.UPDATE) {
					SpringService.getBean(ClubDaoService.class).getDao().updateClubBulletin(bulletinModel);
					bulletinModel.setNeedDB(false);
				}

				ClubBulletinWrap wrap = new ClubBulletinWrap(bulletinModel);
				ClubService.getInstance().getSharedbulletins().put(bulletinModel.getId(), wrap);
				ClubService.getInstance().clubs.forEach((id, club) -> {
					club.runInReqLoop(() -> {
						Utils.bulletinEvent(0, club, wrap.getId(), type);
					});
				});
			} else if (model.getCategory() == SSHEBulletinVO.CATEGORY_APPOINT) {
				List<Integer> clubList = model.getClubIds();
				List<ClubBulletinModel> list = new ArrayList<>();
				for (Integer clubId : clubList) {
					ClubBulletinModel tmpModel = new ClubBulletinModel();
					tmpModel.setClubId(clubId);
					tmpModel.setCategory(bulletinModel.getCategory());
					tmpModel.setCreatorId(bulletinModel.getCreatorId());
					tmpModel.setStartDate(bulletinModel.getStartDate());
					tmpModel.setEndDate(bulletinModel.getEndDate());
					tmpModel.setStatus(bulletinModel.getStatus());
					tmpModel.setText(bulletinModel.getText());
					tmpModel.setId(bulletinModel.getId());
					if (type == ClubBulletinCode.CREATE) {
						SpringService.getBean(ClubDaoService.class).getDao().insertClubBulletin(tmpModel);
					} else if (type == ClubBulletinCode.UPDATE) {
						SpringService.getBean(ClubDaoService.class).getDao().updateClubBulletin(tmpModel);
						tmpModel.setNeedDB(false);
					}

					list.add(tmpModel);
				}
				Map<Integer, List<ClubBulletinModel>> clubBulletinModesMaps = list.stream().collect(groupingBy(ClubBulletinModel::getClubId));
				for (Integer clubId : clubList) {
					Club club = ClubService.getInstance().getClub(clubId);
					if (null == club) {
						continue;
					}
					club.runInReqLoop(() -> {
						ClubBulletinModel tempModel = clubBulletinModesMaps.get(clubId).get(0);
						ClubBulletinWrap wrap = new ClubBulletinWrap(tempModel);
						club.bulletins.put(tempModel.getId(), wrap);
						Utils.bulletinEvent(0, club, wrap.getId(), type);
					});
				}
			}
			isSuccess = true;
		}
		return isSuccess;
	}
}
