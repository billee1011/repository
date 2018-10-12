class CurrencyUnit {
	public static _instance:CurrencyUnit;
	public static get instance():CurrencyUnit
	{
		if(!this._instance){
			this._instance = new CurrencyUnit();
		}
		return this._instance;
	}
	public constructor() {
		this.currencyList = {};
		this.currencyIconObj = {};
	}
	private currencyIconObj:Object;
	private currencyList:Object;
	/**
	 * 0 元宝
	1 金币
	2 精力
	3 威望
	4 勇气
	5 战功
	6 熔炼值
	 */
	public updateCurrency(type:number,value:number):void
	{
		this.currencyList[type] = value;
	}

	public getCurrency(type:number):number
	{
		var v:number = this.currencyList[type];
		if(!v){
			v = 0;
		}
		return v;
	}
	public getGold():number
	{
		var v:number = this.currencyList[ConstDefine.CURRENCY_GOLD];
		if(!v){
			v = 0;
		}
		return v;
	}
	public getMoney():number
	{
		var v:number = this.currencyList[ConstDefine.CURRENCY_MONEY];
		if(!v){
			v = 0;
		}
		return v;
	}

	/*public getItemvoByCurrencyType(type:number):ItemVO
	{
		if(!this.currencyIconObj[type]){
			var str:string = ConfigDB.instance.getValue('12');
			var list:Array<any> = str.split(';');
			for (var key in list) {
				var a:string = list[key];
				var b:Array<any> = a.split(':');
				this.currencyIconObj[b[0]] = b[1];
			}
		}
		var id:number = this.currencyIconObj[type];
		return ItemVoUnit.getNewItemVo(id);
	}*/
}