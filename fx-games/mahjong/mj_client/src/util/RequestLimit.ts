class RequestLimit {
	public static _instance:RequestLimit;
	public static get instance():RequestLimit
	{
		if(!this._instance){
			this._instance = new RequestLimit();
		}
		return this._instance;
	}
	private record:Object;
	public constructor() {
		this.record = {};
	}

	public check(fun:any,limitTime:number=500):boolean
	{
		if(!this.record[fun]){
			this.record[fun] = egret.getTimer();
			return true;
		}
		else if(egret.getTimer()-this.record[fun] >= limitTime)
		{
			this.record[fun] = egret.getTimer();
			return true;
		}else{
			//this.record[fun] = egret.getTimer();
			return false;
		}
	}
}