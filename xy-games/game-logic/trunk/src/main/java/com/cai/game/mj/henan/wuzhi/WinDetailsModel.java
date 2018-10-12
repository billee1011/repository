package com.cai.game.mj.henan.wuzhi;

import java.util.ArrayList;
import java.util.List;

public class WinDetailsModel {
	public List<WinDetailModel> winDetailList = new ArrayList<>();
}

class WinDetailModel {
	public int type;
	public int card;
	public int provider;
	public int score;
}
