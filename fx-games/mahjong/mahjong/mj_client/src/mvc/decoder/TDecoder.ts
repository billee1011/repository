class TDecoder {
	protected types:Array<number>;
	protected facade:Facade;

	public constructor() {
	}

	public init(facade:Facade):void
	{
		this.facade = facade;
		var num:number;
		for (var key in this.types) {//types[10000,10001...]
			num = this.types[key];
			//if (this.hasOwnProperty('f_'+num)) {
			facade.registerProtocol(num,[this['f_'+num],this])
			//}
		}
		
	}

	protected checkSucc(arr:Array<any>):boolean
	{
		if(arr[0] == 0){
			var t:string = CodeDB.instance.getDes(arr[1]);
			t = t ? t : '不存在的code:'+arr[1];
			Tnotice.instance.popUpTip(t);
			return false;
		}
		return true;
	}
}