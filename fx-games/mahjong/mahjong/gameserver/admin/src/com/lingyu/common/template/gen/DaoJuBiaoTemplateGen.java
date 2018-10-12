package com.lingyu.common.template.gen;

import com.lingyu.common.template.AMF3;
import com.lingyu.common.template.Attribute;

@AMF3(res = "DaoJuBiao.jat")
public abstract class DaoJuBiaoTemplateGen {
	@Attribute("addcoin")
	private int addcoin;

	/** 获得金币 */
	public int getAddcoin() {
		return addcoin;
	}

	@Attribute("addexp")
	private int addexp;

	/** 增加人物经验 */
	public int getAddexp() {
		return addexp;
	}

	@Attribute("addhpnumber")
	private int addhpnumber;

	/** 生命值增加数值 */
	public int getAddhpnumber() {
		return addhpnumber;
	}

	@Attribute("addlv")
	private String addlv;

	/** 提升等级 */
	public String getAddlv() {
		return addlv;
	}

	@Attribute("addmpnumber")
	private int addmpnumber;

	/** 魔法值增加数值 */
	public int getAddmpnumber() {
		return addmpnumber;
	}

	@Attribute("addprestige")
	private int addprestige;

	/** 增加声望 */
	public int getAddprestige() {
		return addprestige;
	}

	@Attribute("addsilver")
	private int addsilver;

	/** 获得礼金 */
	public int getAddsilver() {
		return addsilver;
	}

	@Attribute("addyuanbao")
	private int addyuanbao;

	/** 获得元宝 */
	public int getAddyuanbao() {
		return addyuanbao;
	}

	@Attribute("addzhenqi")
	private int addzhenqi;

	/** 增加灵气值 */
	public int getAddzhenqi() {
		return addzhenqi;
	}

	@Attribute("autodrop")
	private String autodrop;

	/** 是否自动拾取 */
	public String getAutodrop() {
		return autodrop;
	}

	@Attribute("autouse")
	private int autouse;

	/** 自动使用 */
	public int getAutouse() {
		return autouse;
	}

	@Attribute("banghuilv")
	private int banghuilv;

	/** 使用该物品的帮会等级需求 */
	public int getBanghuilv() {
		return banghuilv;
	}

	@Attribute("baoshilv")
	private int baoshilv;

	/** 使用该物品的宝石阶数需求 */
	public int getBaoshilv() {
		return baoshilv;
	}

	@Attribute("bianshenlv")
	private int bianshenlv;

	/** 使用要求需要72变阶数 */
	public int getBianshenlv() {
		return bianshenlv;
	}

	@Attribute("bianshenneed")
	private String bianshenneed;

	/** "使用需要专用对应神将阶数神将ID|阶数" */
	public String getBianshenneed() {
		return bianshenneed;
	}

	@Attribute("buffid")
	private String buffid;

	/** 使用后获得效果ID */
	public String getBuffid() {
		return buffid;
	}

	@Attribute("category")
	private String category;

	/** 所属的物品类型 */
	public String getCategory() {
		return category;
	}

	@Attribute("cd1")
	private String cd1;

	/** 使用时间间隔 */
	public String getCd1() {
		return cd1;
	}

	@Attribute("cd2")
	private String cd2;

	/** 公共CD */
	public String getCd2() {
		return cd2;
	}

	@Attribute("chibanglv")
	private int chibanglv;

	/** 使用该物品的翅膀阶数需求 */
	public int getChibanglv() {
		return chibanglv;
	}

	@Attribute("collecttype")
	private String collecttype;

	/** 拾取类型 */
	public String getCollecttype() {
		return collecttype;
	}

	@Attribute("consumeorder")
	private int consumeorder;

	/** 消耗顺序 */
	public int getConsumeorder() {
		return consumeorder;
	}

	@Attribute("count")
	private String count;

	/** 每日掉落数量限制 */
	public String getCount() {
		return count;
	}

	@Attribute("critical")
	private String critical;

	/** 暴击 */
	public String getCritical() {
		return critical;
	}

	@Attribute("describe")
	private String describe;

	/** 文字描述 */
	public String getDescribe() {
		return describe;
	}

	@Attribute("dropsound")
	private String dropsound;

	/** 掉落时采用的声音 */
	public String getDropsound() {
		return dropsound;
	}

	@Attribute("dropurl")
	private String dropurl;

	/** 物品掉落效果 */
	public String getDropurl() {
		return dropurl;
	}

	@Attribute("dropxianshi")
	private boolean dropxianshi;

	/** 掉落是否显示 */
	public boolean getDropxianshi() {
		return dropxianshi;
	}

	@Attribute("duration")
	private int duration;

	/** 物品有效期 */
	public int getDuration() {
		return duration;
	}

	@Attribute("effects")
	private String effects;

	/** 图标发光特效 */
	public String getEffects() {
		return effects;
	}

	@Attribute("exprate")
	private float exprate;

	/** 经验倍率 */
	public float getExprate() {
		return exprate;
	}

	@Attribute("faguang")
	private int faguang;

	/** 在背包总是否边缘发光 */
	public int getFaguang() {
		return faguang;
	}

	@Attribute("fbaddexp")
	private long fbaddexp;

	/** 增加法宝经验值 */
	public long getFbaddexp() {
		return fbaddexp;
	}

	@Attribute("guajitype")
	private int guajitype;

	/** 挂机物品类型 */
	public int getGuajitype() {
		return guajitype;
	}

	@Attribute("guangyi")
	private String guangyi;

	/** 类型49时，使用后获得第几阶光翼 */
	public String getGuangyi() {
		return guangyi;
	}

	@Attribute("hitpoints")
	private String hitpoints;

	/** 最大HP */
	public String getHitpoints() {
		return hitpoints;
	}

	@Attribute("id")
	private String id;

	/** 物品id */
	public String getId() {
		return id;
	}

	@Attribute("itemlevel")
	private int itemlevel;

	/** 物品级别 */
	public int getItemlevel() {
		return itemlevel;
	}

	@Attribute("jhzqjn")
	private String jhzqjn;

	/** 使用后激活对应坐骑技能 */
	public String getJhzqjn() {
		return jhzqjn;
	}

	@Attribute("job")
	private String job;

	/** 使用该物品的职业需求 */
	public String getJob() {
		return job;
	}

	@Attribute("kouchuyb")
	private int kouchuyb;

	/** 元宝扣除 */
	public int getKouchuyb() {
		return kouchuyb;
	}

	@Attribute("lasttime")
	private long lasttime;

	/** 效果时间（分钟） */
	public long getLasttime() {
		return lasttime;
	}

	@Attribute("levelreq")
	private int levelreq;

	/** 使用该物品的级别需求 */
	public int getLevelreq() {
		return levelreq;
	}

	@Attribute("mana")
	private String mana;

	/** 最大MP */
	public String getMana() {
		return mana;
	}

	@Attribute("max")
	private int max;

	/** 可使用最大个数 */
	public int getMax() {
		return max;
	}

	@Attribute("maxdrop")
	private String maxdrop;

	/** 怪物爆出件数上限阀值（填0为不限制）（格式：件数|天数）例如：2|3 是指：每3天最多爆出2件 */
	public String getMaxdrop() {
		return maxdrop;
	}

	@Attribute("maxstack")
	private int maxstack;

	/** 最大叠放数量(默认:1) */
	public int getMaxstack() {
		return maxstack > 0 ? maxstack : 1;
	}

	@Attribute("meiren")
	private String meiren;

	/** 类型44时，使用后获得美人 */
	public String getMeiren() {
		return meiren;
	}

	@Attribute("name")
	private String name;

	/** 物品名字 */
	public String getName() {
		return name;
	}

	@Attribute("obtainlocked")
	private int obtainlocked;

	/** 拾取绑定 1.绑定 */
	public boolean isObtainlocked() {
		return obtainlocked == 1;
	}

	@Attribute("ownonly")
	private String ownonly;

	/** 拥有唯一 */
	public String getOwnonly() {
		return ownonly;
	}

	@Attribute("physicdamage")
	private String physicdamage;

	/** 攻击 */
	public String getPhysicdamage() {
		return physicdamage;
	}

	@Attribute("physicdefence")
	private String physicdefence;

	/** 防御 */
	public String getPhysicdefence() {
		return physicdefence;
	}

	@Attribute("pifenglv")
	private int pifenglv;

	/** 使用该物品的披风阶数需求 */
	public int getPifenglv() {
		return pifenglv;
	}

	@Attribute("pkzhi")
	private int pkzhi;

	/** 消除PK值 */
	public int getPkzhi() {
		return pkzhi;
	}

	@Attribute("qibinglv")
	private int qibinglv;

	/** 使用该物品的骑兵阶数需求 */
	public int getQibinglv() {
		return qibinglv;
	}

	@Attribute("rareitem")
	private String rareitem;

	/** 是否珍稀物品（获得时有额外提示） */
	public String getRareitem() {
		return rareitem;
	}

	@Attribute("rarelevel")
	private int rarelevel;

	/** 物品稀有等级 */
	public int getRarelevel() {
		return rarelevel;
	}

	@Attribute("recycle")
	private int recycle;

	/** 回收价格 */
	public int getRecycle() {
		return recycle;
	}

	@Attribute("shenjiang")
	private String shenjiang;

	/** 使用后获得神将 */
	public String getShenjiang() {
		return shenjiang;
	}

	@Attribute("skill")
	private int functionType;

	/** 物品的功能类型 */
	public int getFunctionType() {
		return functionType;
	}

	@Attribute("specialtype")
	private int specialtype;

	/** 特殊类型 */
	public int getSpecialtype() {
		return specialtype;
	}

	@Attribute("splocked")
	private int splocked;

	/** 特殊绑定 */
	public int getSplocked() {
		return splocked;
	}

	@Attribute("sxbfb")
	private float sxbfb;

	/** 坐骑属性增加百分比 */
	public float getSxbfb() {
		return sxbfb;
	}

	@Attribute("url")
	private String url;

	/** 图标资源 */
	public String getUrl() {
		return url;
	}

	@Attribute("useable")
	private String useable;

	/** 是否可直接使用 */
	public String getUseable() {
		return useable;
	}

	@Attribute("useallable")
	private int useallable;

	/** 是否可使用全部 */
	public int getUseallable() {
		return useallable;
	}

	@Attribute("usefullife")
	private String usefullife;

	/** "使用有效期（秒）" */
	public String getUsefullife() {
		return usefullife;
	}

	@Attribute("usetip")
	private int usetip;

	/** 使用提示 */
	public int getUsetip() {
		return usetip;
	}

	@Attribute("usetype")
	private String usetype;

	/** 打开UI型的类型 */
	public String getUsetype() {
		return usetype;
	}

	@Attribute("vipdian")
	private int vipdian;

	/** 使用后获得VIP点数 */
	public int getVipdian() {
		return vipdian;
	}

	@Attribute("viplv")
	private int viplv;

	/** 使用该物品的VIP等级需求 */
	public int getViplv() {
		return viplv;
	}

	@Attribute("volt")
	private String volt;

	/** 闪避 */
	public String getVolt() {
		return volt;
	}

	@Attribute("whethercost")
	private int whethercost;

	/** 是否可出售 */
	public int getWhethercost() {
		return whethercost;
	}

	@Attribute("xianjingtili")
	private int xianjingtili;

	/** 增加仙境体力 */
	public int getXianjingtili() {
		return xianjingtili;
	}

	@Attribute("xuni")
	private boolean xuni;

	/** 是否为虚拟物品进入虚拟背包 */
	public boolean isXuni() {
		return xuni;
	}

	@Attribute("zhanlilv")
	private int zhanlilv;

	/** 使用该物品的角色战斗力需求 */
	public int getZhanlilv() {
		return zhanlilv;
	}

	@Attribute("zhenbao")
	private int zhenbao;

	/** 是否支持珍宝双倍0不是1是 */
	public int getZhenbao() {
		return zhenbao;
	}

	@Attribute("zhenqijs")
	private String zhenqijs;

	/** 灵气减少% */
	public String getZhenqijs() {
		return zhenqijs;
	}

	@Attribute("zqshengxian")
	private int zqshengxian;

	/** 坐骑升仙等级 */
	public int getZqshengxian() {
		return zqshengxian;
	}

	@Attribute("zqzhufuxianshi")
	private String zqzhufuxianshi;

	/** 坐骑祝福值显示 */
	public String getZqzhufuxianshi() {
		return zqzhufuxianshi;
	}

	@Attribute("zuoqiattackrate")
	private String zuoqiattackrate;

	/** 坐骑攻击速度 */
	public String getZuoqiattackrate() {
		return zuoqiattackrate;
	}

	@Attribute("zuoqicritical")
	private String zuoqicritical;

	/** 坐骑暴击 */
	public String getZuoqicritical() {
		return zuoqicritical;
	}

	@Attribute("zuoqihitpoints")
	private String zuoqihitpoints;

	/** 最大坐骑HP */
	public String getZuoqihitpoints() {
		return zuoqihitpoints;
	}

	@Attribute("zuoqijilv")
	private float zuoqijilv;

	/** 坐骑进阶成功率增加 */
	public float getZuoqijilv() {
		return zuoqijilv;
	}

	@Attribute("zuoqilv")
	private int zuoqilv;

	/** 使用该物品的坐骑阶数需求 */
	public int getZuoqilv() {
		return zuoqilv;
	}

	@Attribute("zuoqimana")
	private String zuoqimana;

	/** 最大坐骑MP */
	public String getZuoqimana() {
		return zuoqimana;
	}

	@Attribute("zuoqiphysicdamage")
	private String zuoqiphysicdamage;

	/** 坐骑攻击 */
	public String getZuoqiphysicdamage() {
		return zuoqiphysicdamage;
	}

	@Attribute("zuoqiphysicdefence")
	private String zuoqiphysicdefence;

	/** 坐骑防御 */
	public String getZuoqiphysicdefence() {
		return zuoqiphysicdefence;
	}

	@Attribute("zuoqivelocity")
	private String zuoqivelocity;

	/** 坐骑移动速度 */
	public String getZuoqivelocity() {
		return zuoqivelocity;
	}

	@Attribute("zuoqivolt")
	private String zuoqivolt;

	/** 坐骑闪避 */
	public String getZuoqivolt() {
		return zuoqivolt;
	}

	@Attribute("searchType")
	private int searchType;

	/** 搜索类型 */
	public int getSearchType() {
		return searchType;
	}

	@Attribute("haogandu")
	private int haogandu;

	/** 好感度 */
	public int getHaogandu() {
		return haogandu;
	}

	@Attribute("petLikeType")
	private int petLikeType;

	/** 宠物喜好物类型 */
	public int getPetLikeType() {
		return petLikeType;
	}

	@Attribute("compoundId")
	private int compoundId;

	/** 激活配方ID */
	public int getCompoundId() {
		return compoundId;
	}
	
	@Attribute("moneyType")
	private int moneyType;

	/** 允许使用的货币类型类型 */
	public int getMoneyType() {
		return moneyType;
	}
	
	@Attribute("mailMoney")
	private int mailMoney;

	/** 邮寄提取价格 */
	public int getMailMoney() {
		return mailMoney;
	}
	
	@Attribute("validUseArea")
	private String validUseArea;
	
	/** 使用物品位置限制 */
	protected String getValidUseAreaStr() {
		return validUseArea;
	}
	
	@Attribute("useCostValue")
	private int useCostValue;
	
	/**	特殊消费(目前有被合成) */
	public int getUseCostValue(){
		return useCostValue;
	}
	
	@Attribute("useCostType")
	private int useCostType;
	
	/**	特殊消费类型 (目前有被合成)*/
	public int getUseCostType(){
		return useCostType;
	}
	/** 可否跨服出售 */
	@Attribute("sale")
	private int sale;
	
	protected int getSale(){
		return sale;
	}
}