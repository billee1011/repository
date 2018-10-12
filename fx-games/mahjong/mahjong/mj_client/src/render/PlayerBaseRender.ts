class PlayerBaseRender extends RenderBase{
	public constructor() {
		super('');
	}
	protected head:RoleHeadRender;
	protected vo:HeroVO;
	protected ownCardList:HPagelist;
	protected duiList:HPagelist;
	protected playedList:HPagelist;
	protected isMain:boolean;
	protected opEffectItem:OperateEffectItem;
	protected uiLoadComplete():void
    {
       this.head = new RoleHeadRender();
	  this.addChild(this.head);

	 

	  this.opEffectItem = new OperateEffectItem();

	   this.childInit();
	   
	   this.isMain = false;
    }
	protected childInit():void
	{
		//override
	}

	protected dataChanged():void
	{
		this.vo = this.data;

		this.head.data = this.vo;

		this.refreshOwnCard();
		
		this.refreshDuiCard();

		this.refreshPlayedCard();


		this.layout();

		/*if(GlobalDefine.gameState == 0){
			this.showReadyHand();
		}*/
		if(this.vo.ready){
			this.showReadyHand();
		}
	}

	public refreshDuiCard():void
	{
		if(this.duiList){
			this.duiList.displayList(this.vo.duiCardList);
			this.refreshOwnCardPosition();
		}
	}
	/**
	 * 对过牌后 该干啥干啥
	 */
	protected refreshOwnCardPosition():void
	{

	}

	/**
	 * 
	 */
	public refreshPlayedCard():void
	{
		if(this.playedList){
			this.playedList.displayList(this.vo.playedCardList);
		}
	}
	public addVoToOwnCardList(vo:CardVO):void
	{
		this.vo.ownCardList.push(vo);
	}
	/**
	 * 对牌添加方式
	 */
	public addVoToduiCardList(vo:CardVO):void
	{
		this.vo.duiCardList.push(vo);
	}
	/**
	 * 因为有些是从数组前面添加，固可重写此方法
	 */
	public addVoToPlayedCardList(vo:CardVO):void
	{
		this.vo.playedCardList.push(vo);
	}
	/**
	 * 出牌后先隐藏，待动画播放完毕再显示这张牌
	 */
	public setNowPlayedCardHide(id:number):egret.Point
	{
		//this.refreshPlayedCard();
		var render:PlayedCardRender;
		var t:any[] = this.playedList.getAllItem();
		for (var key in t) {
			render = t[key];
			if(render.data.id == id){
				render.visible = false;
				return render.localToGlobal(0,0);
			}
		}
		return null;
	}
	/**
	 * 动画播放完毕显示牌面
	 */
	public setNowPlayedCardShow(id:number):void
	{
		var render:PlayedCardRender;
		var t:any[] = this.playedList.getAllItem();
		for (var key in t) {
			render = t[key];
			if(render.data.id == id){
				render.visible = true;
				render.showSign();
				break;
			}
		}
	}

	public refreshOwnCard():void
	{
		if(this.ownCardList){
			this.ownCardList.displayList(this.vo.ownCardList);
			this.ownCardList.refreshOther();
		}
	}

	protected layout():void
	{
		//override
	}

	public showReadyHand():void
	{
		this.head.readyPic.visible = true;
	}
	public hideReadyHand():void
	{
		this.head.readyPic.visible = false;
	}

	public showOperateEffect(type:number):void
	{
		this.opEffectItem.playEffect(type);
		this.addChild(this.opEffectItem);
	}
	/*public clearData():void
	{
		if(this)
	}*/
}