/**
 * 
 */
package com.cai.common.define;

/**
 * @author xwy
 *
 *1：天胡【滚滚胡不算】：
2：杠上花：
3：满天飞：
4：碰碰胡：
5:  海底捞月：
6：起手单招：
7：起手双招：
8：门清自摸：
 */
public enum ECardType {
	// =======玩家的============
	anLong("anlong","起手暗龙"),
	hongZhong4("hongZhong4","四个红中"),
	haidi("haidi","海底捞月"),
	qixiaodui("qixiaodui","七小对"),
	shidui("shidui","10对"),
	hong("hong","红原"),
	hei("hei","黑原"),
	ku("ku","枯胡"),
	zhong("zhong","重胡"),
	tai("tai","台胡"),
	ka("ka","卡胡"),
	qing("qing","清胡"),
	tianhu("tianhu","天胡"),
	mantianfei("mantianfei","满天飞"),
	pengpenghu("pengpenghu","碰碰胡"),
	anLong2("anLong2","起手2暗龙"),
	zimomq("zimomq","门青自摸"),
	gangshanghua("gangshanghua","福禄寿杠上花"),
	
	
	//河南麻将
	sihun("sihun","河南四混"),
	henangang("henangang","河南抢杠胡"),
	henanqidui("henanqidui","河南七对"),
	henanqiduihaohua("henanqiduihaohua","河南豪华七对"),
	henankaihua("henankaihua","河南杠开"),
	henan4hong("henan4hong","河南4红中"),
	
	
	
	//湖北麻将
	heimo("heimo","黑摸"),
	ruanmo("ruanmo","软摸"),
	zhuotong("zhuotong","捉铳"),
	rechong("rechong","热冲"),
	
	//湖南麻将
	mjgangshanghua("mjgangshanghua","麻将杠上花"),
	smjgangshanghua("smjgangshanghua","麻将双杠上花"),
	mjgangshangpao("mjgangshangpao","麻将杠上炮"),
	smjgangshangpao("smjgangshangpao","麻将双杠上炮"),
	qiangganghu("qiangganghu","抢杠胡"),
	qidui("qidui","七对"),
	haohuaqidui("haohuaqidui","豪华七对"),
	shaohuaqidui("shaohuaqidui","双豪华七对"),
	qishouhu("qishouhu","起手胡"),
	hongzhong4("hongzhong4","4红中"),
	mjpph("mjpph","麻将碰碰胡"),
	jjh("jjh","将将胡"),
	qingyise("qingyise","清一色胡"),
	cshaidilao("cshaidilao","长沙海底捞"),
	cshaidipao("cshaidipao","长沙海底炮"),
	quanqiuren("quanqiuren","全球人"),
	liuliushun("liuliushun","六六顺"),
	
	//红黑胡
	hhtianhu("tianhu","天胡"),
	hhdihu("dihu","地胡"),
	hhyidianhong("yidianhong","一点红"),
	hhhonghu("honghu","红胡"),
	hhhongfantian("hongfantian","红翻天"),
	hhallhei("allhei","全黑"),
	hhhaihu("haihu","海胡"),
	hhtinghu("tinghu","听胡"),
	hhdahu("dahu","大胡"),
	hhxiaohu("xiaohu","小胡"),
	hhduizihu("duizihu","对子胡"),
	hhshuahou("shuahou","耍猴"),
	hhhuangfan("huangfan","黄番"),
	hhtuanyuan("tuanyuan","团圆"),
	hhhanghangxi("hanghangxi","行行息"),
	
	//牛牛
	oxwuxiaoox("wuxiaoox","五小牛"),
	oxwuhuaox("wuhuaox","五花牛"),
	oxboomox("boomox","炸弹牛"),
	
	//21点
	hjkaa("aa","AA"),
	hjkhjk("","HJK"),
	hjk777("777","777"),
	hjkwuxiaolong("wuxiaolong","五小龙"),
	
	;

	private String id;

	private String desc;

	ECardType(String id, String desc) {
		this.id = id;
		this.desc = desc;
	}

	public static ECardType ECardType(String id) {
		for (ECardType c : ECardType.values()) {
			if (c.id == id)
				return c;
		}
		return null;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

}
