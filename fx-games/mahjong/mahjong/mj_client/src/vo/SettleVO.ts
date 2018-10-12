class SettleVO {
	public constructor() {
	}

	/**
	 * roleId, 
			jifen总积分, 
			oneGangjifen每局的杠分, 
			oneSumjifen每局的总分,
			[玩家牌的列表
				[
					id,
					color,
					num,
					used
				]...
			]
	 */
	public roleId:number;

	public totalScore:number;

	public curGangScore:number;

	public curScore:number;

	public cardList:any[];
	public decode(arr:any[]):void
	{
		this.roleId = arr[0];
		//this.totalScore = arr[1];
		//this.curGangScore = arr[2];
		this.curScore = arr[1];
		let vo:CardVO;
		this.cardList = [];
		for (var key in arr[2]) {
			vo = new CardVO();
			vo.decode(arr[2][key]);
			this.cardList.push(vo);
		}
	}
}