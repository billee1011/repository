class PlayedCardRender extends RenderBase{
	public constructor() {
		super('');
	}
	private icon:AsyImage;
	private ud:UpdownUtil;
	private model:MjModel;
	protected uiLoadComplete():void
	{
		this.model = TFacade.getProxy(MjModel.NAME);
		this.icon = new AsyImage(0,0,true);
		this.addChild(this.icon);
	}
	private cvo:CardVO;
	protected dataChanged():void
	{
		if(!this.data) return;

		if(!this.icon){
			this.icon = new AsyImage(0,0,true);
			this.addChild(this.icon);
		}

		let vo:CardVO = this.data as CardVO;
		this.cvo = vo;
		let n:string;
		let p:number = this.model.getIconPByPosition(vo.position);
		let tt:number = vo.style == 4 ? vo.type+1-10 : vo.type;
		n = `p${p}s${vo.style}_${tt}`;
		this.icon.setUrl(n);

		if(vo.position == this.model.mainDir){
			this.icon.setScale(0.65,0.65);
		}
		
	}
	public showSign():void
	{
		this.ud = LayerManage.instance.getUpDown();
		this.addChild(this.ud);
		if(this.cvo.position != this.model.mainDir){
			this.ud.x = this.icon.width*.5;
			this.ud.y = -15;
		}
		else{
			this.ud.x = 16;
			this.ud.y = -15;
		}
	}

	public clear():void
	{
		DisplayUtil.removeDisplay(this.ud);
		this.icon.clear();
		DisplayUtil.removeDisplay(this.icon);
		this.icon = null;
	}
}