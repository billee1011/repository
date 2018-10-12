package com.cai.future.runnable;

import com.cai.common.base.BaseTask;
import com.cai.common.domain.GameNoticeModel;
import com.cai.dictionary.NoticeDict;

public class GameNoticeTask extends BaseTask {
	
	private int noticeId;
	
	public GameNoticeTask(int noticeId){
		this.noticeId = noticeId;
	}

	@Override
	public void execute() {
		if(NoticeDict.INSTANCE().isClose(noticeId)){
			NoticeDict.INSTANCE().cancelFuture(noticeId);
			return;
		}
		GameNoticeModel model = NoticeDict.INSTANCE().getModel(noticeId);
		if(model != null){
			NoticeDict.INSTANCE().sendNotice(model);
		}
	}

	@Override
	public String getTaskName() {
		return "GameNoticeTask->";
	}

}
