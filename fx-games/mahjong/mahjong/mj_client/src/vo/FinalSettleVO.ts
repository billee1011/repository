class FinalSettleVO {
	public roleid:number;
	/**
	 * 1,自摸、2,接炮、3,点炮、4,暗杠、5,明杠
	 */
	public countList:any[];
	public index:number;
	public name:string;
	public score:number;
	public constructor() {
	}

	public decode(arr:any[]):void
	{
		this.countList = [];
		for(let i:number=0; i<arr.length; i++)
		{
			this.countList.push([i+1,arr[i]]);
		}
	}
}