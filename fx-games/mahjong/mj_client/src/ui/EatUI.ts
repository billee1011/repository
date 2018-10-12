class EatUI extends UIBase{
	public static NAME:string = 'EatUI';
	public constructor() {
		super('');
		this.isAloneShow = true;
		this.centerFlag = true;
	}
	private list:HPagelist;
	private model:MjModel;
	protected uiLoadComplete():void
	{
		this.y = GlobalDefine.stageH*0.88;

		this.model = TFacade.getProxy(MjModel.NAME);
		this.list = new HPagelist(EatRender,220,84,false);
		this.addChild(this.list);
		this.list.addEventListener(HPagelist.RENDER_CHANGE,this.renderClick,this)
	}
	private ownList:any[];

	private renderClick(evt:CurEvent):void
	{
		let data:any = this.list.currentItem.data;
		var t:any[] = [];
		for (var key in data) {
			let vo:CardVO = data[key];
			if(vo.id != this.model.playingCard.id){
				t.push(vo.id);
			}
		}
		SendOperate.instance.requestEatCard(t);
		this.hide();
	}
	/**
	 * 点击吃标签后弹出，没事别乱弹
	 */
	protected doExecute():void
	{
		var cur:CardVO = this.model.playingCard;
		var hvo:HeroVO = this.model.getHeromsg()[GlobalDefine.herovo.roleId];
		this.ownList = hvo.ownCardList;

		//筛选出 可以吃的牌的情况  //痞子癞子不能参与吃

		let farr:any[] = [];

		let a:CardVO = this.getCard(cur.style,cur.type+1);
		let b:CardVO = this.getCard(cur.style,cur.type+2);
		if(a && b){//x,a,b
			farr.push([cur,a,b]);
		}
		let c:CardVO = this.getCard(cur.style,cur.type-1);
		if(a && c){//
			farr.push([c,cur,a]);
		}
		let d:CardVO = this.getCard(cur.style,cur.type-2);
		if(c && d){
			farr.push([d,c,cur]);
		}

		this.list.displayList(farr);

		this.outIndex = egret.setTimeout(this.layout,this,100);
		this.list.visible = false;
	}
	private outIndex:number;
	private layout():void
	{
		egret.clearTimeout(this.outIndex);
		this.x = GlobalDefine.stageW*.5-this.list.width*.5;
		this.list.visible = true;
		this.y = GlobalDefine.stageH*0.68;
	}



	private getCard(style:number,value:number):CardVO
	{
		let temp:CardVO;
		for(let i:number=0; i<this.ownList.length; i++){
			temp = this.ownList[i];
			if(temp.style != style) continue;
			if(style == this.model.card_pizi.style && value == this.model.card_pizi.type) continue; //不能是痞子
			if(style == this.model.card_laizi.style && value == this.model.card_laizi.type) continue;//不能是癞子
			if(temp.style == style && temp.type == value) {
				return temp;
			}
		}
		return null;
	}

	protected backGroundClick():void
	{

	}
}