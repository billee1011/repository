class CodeDB {
	public constructor() {
	}

	public static instance:CodeDB = new CodeDB();

	private _obj:Object;
	public decode(arr:any[]):void
	{
		this._obj = {};
		for (var key in arr) {
			var temp:any = arr[key];
			this._obj[temp.code] = temp.des;
		}
	}
	public getDes(code:number):string
	{
		return this._obj[code];
	}
}