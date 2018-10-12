/***
 * 每位玩家手上的牌
 */
class CardRender extends RenderBase{
	public constructor() {
		super('');
	}
	private icon:AsyImage;
	private model:MjModel;
	private cvo:CardVO;
	private dir:number;
	private plIcon:eui.Image;
	protected uiLoadComplete():void
	{
		this.model = TFacade.getProxy(MjModel.NAME);
		this.plIcon = new eui.Image;
		this.addChild(this.plIcon);
	}
	protected dataChanged():void
	{
		if(!this.data) return;
		this.plIcon.source = null;
		if(!this.icon){
			this.icon = new AsyImage(0,0,true);
			this.addChildAt(this.icon,0);
			this.icon.touchEnabled = this.icon.touchChildren = false;
		}

		let vo:CardVO = this.data as CardVO;
		this.cvo = vo;
		let n:string;
		if(vo.position == this.model.mainDir){
			let style:string = this.model.getIconStyle(vo.position);
			let p:number = this.model.getIconPByPosition(vo.position);
			let tt:number = vo.style == 4 ? vo.type+1-10 : vo.type;
			n = `p${p}${style}${vo.style}_${tt}`;
			this.icon.setUrl(n);

			if(this.model.card_pizi.style == vo.style && this.model.card_pizi.type == vo.type){
				this.plIcon.visible = true;
				this.plIcon.source = SheetManage.getTextureFromCenter('icon_pizi');
			}
			if(this.model.card_laizi.style == vo.style && this.model.card_laizi.type == vo.type){
				this.plIcon.visible = true;
				this.plIcon.source = SheetManage.getTextureFromCenter('icon_laizi');
			}
			this.plIcon.x = 53;
			this.plIcon.y = 2;
		}
		else{
			let dir = this.model.getIconPByPosition(vo.position);
			n = `tbgs_${dir}`;
			this.icon.setUrl(n);
			this.plIcon.visible = false;
		}
		this.dir = vo.position;
	}
	public refreshOther():void
	{
		this.offsetLastCard();
	}
	private offsetLastCard():void
	{
		if(!this.cvo.justGet){
			return;
		}
		var gap:number = 20;
		var mdir:number = this.model.mainDir;
		if(this.model.checkIsTop(mdir,this.dir)){
			this.x -= gap;
		}
		else if(this.model.checkIsLeft(mdir,this.dir)){
			this.y += gap;
		}
		else if(this.model.checkIsRight(mdir,this.dir)){
			this.y -= gap+8;
		}
		else{
			this.x += gap;
		}
	}
	public clear():void
	{
		DisplayUtil.removeDisplay(this.icon);
		this.icon = null;
	}
}