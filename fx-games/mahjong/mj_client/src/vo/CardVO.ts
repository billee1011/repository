class CardVO {
	/**
	 * 1东2南3西4北
	 */
	public position:number;
	/**
	 * 1：万子
	 * 2：股子
	 * 3：条子
	 * 4：风子（东南西北红中发财光板）
	 */
	public style:number;
	/**
	 * 1-9
	 */
	public type:number;

	public id:number;
	/**
	 * 碰过或者杠过
	 */
	public used:boolean;
	/**
	 * 如果是刚刚摸的牌，让它缓缓 不急进入队列，出过牌后 再排列起来
	 */
	public justGet:boolean;
	
	public constructor() {
	}

	public decode(temp:any[]):void
	{
		if(!temp) return;
		this.id = temp[0];
		this.style = temp[1];
		this.type = temp[2];
		this.used = temp[3];
	}
}