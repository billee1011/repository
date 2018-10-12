class ItemVO{
	public guid:number = 0;
	public id:number = 0;
	public name:string = '';
	public level:number = 0;
	public job:number = 0;
	public count:number = 0;
	/**
	 * 性别  0：通用  1：男  2：女
	 */
	public gender:number = 0;
	public tips:string = '';
	public script:string = '';

	public packages:string = '';
	/**
	 * 物品类型
		0 普通物品
		1 消耗品
		2 货币类
		3 宝箱类道具
		25 装备
	 */
	public type:number = 0;
	/**
	 * 道具使用打开的面板
	 */
	public open_ui:string = '';
	/**
	 * type :( 25 装备， 此值为部位)
	 */
	public param1:number = 0;
	/**
	 * 品质
	 */
	public quality:number = 0;
	/**
	 * 叠加数量
	 */
	public stack:number = 0;
	/**
	 * icon编号
	 */
	public looks:number = 0;
	/**
	 * 外观
	 */
	public avatar:number = 0;
	
	public combat:number = 0;
	/**
	 * 基础战力（不含强化等） 用于快速换装提示
	 */
	public baseCombat:number=0;

	//-----------------runData--------
	public strongMsg:any;
	public jinjieMsg:any;
	public upStarMsg:any;
	//-----------------runData---------

	public slot:number = 0;
	/**
	 * 是否允许批量使用 1:允许
	 */
	public batch_use:number = 0;

	public constructor() {
	}
	/**
	 * guid,
		id,
		count,
		quality,
		qianghua:[],
		jinjie,
		shengxing,
		combat,
		baseCombat,
		slot
		...
	 */
	public decodeRunData(data:Array<any>):void
	{
		var i:number = 0;
		this.guid = data[i++];
		this.id = data[i++];
		this.count = data[i++];
		this.quality = data[i++];
		this.strongMsg = data[i++];
		this.jinjieMsg = data[i++];
		this.upStarMsg = data[i++];
		this.combat = data[i++];
		this.baseCombat = data[i++];
		this.slot = data[i++];
	}
}