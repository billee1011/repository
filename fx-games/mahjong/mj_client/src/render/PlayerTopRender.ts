class PlayerTopRender extends PlayerBaseRender{
	public constructor() {
		super();
	}
	private horNum:number;
	protected childInit():void
	{
		this.horNum = 13;

		this.ownCardList = new HPagelist(CardRender,38,59,false);
		this.ownCardList.updateXY(55,4);
		this.ownCardList.rightLeft = true;
		this.addChild(this.ownCardList);

		this.duiList = new HPagelist(DuiCardRender,36,55,false);
		this.duiList.updateXY(586,6)
		this.addChild(this.duiList);

		this.playedList = new HPagelist(PlayedCardRender,36,43,true,this.horNum);
		this.playedList.rightLeft = true;
		this.playedList.updateXY(46,92);
		this.addChild(this.playedList);

		this.head.readyPic.x = -45;
		this.head.readyPic.y = 47;

		this.opEffectItem.updateXY(341,125);
		this.head.updateXY(633,0);
	}
	protected layout():void
	{
		
	/*	let len:number = this.vo.playedCardList ? this.vo.playedCardList.length*36:0;
		this.playedList.x = 500-len;*/

		this.refreshOwnCardPosition();
	}

	protected refreshOwnCardPosition():void
	{
		let len:number = this.vo.duiCardList ? this.vo.duiCardList.length*36:0;
		this.duiList.x = 596 - len;
	}



	public refreshPlayedCard():void
	{
		if(this.playedList){
			//var temp:any[] = this.vo.playedCardList.concat();
			//temp.reverse();
			this.playedList.displayList(this.vo.playedCardList);
			/*let len:number = this.vo.playedCardList ? this.vo.playedCardList.length*36:0;
			this.playedList.x = 500-len;*/
		}
	}
	public addVoToOwnCardList(vo:CardVO):void
	{
		this.vo.ownCardList.unshift(vo);
	}
	/***
	 * 右边的 列表咱得往前塞
	 */
	/*public addVoToPlayedCardList(vo:CardVO):void
	{
		this.vo.playedCardList.unshift(vo);
	}*/
}