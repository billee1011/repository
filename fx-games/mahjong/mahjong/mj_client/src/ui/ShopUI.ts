class ShopUI extends UIBase{
	public static NAME:string = 'ShopUI';
	public constructor() {
		super('resource/UI_exml/Shop.exml');

		this.centerFlag = true;
		this.closeOther = false;
		this.isAloneShow = true;
	}
	private list_charge:eui.List;
	private list:PageList;
	protected uiLoadComplete():void
	{
		let sb:ScrollBar = new ScrollBar(this.list_charge);
		this.addChild(sb);

		this.list = new PageList(this.list_charge,ShopRender);
		this.list.displayList([88,99,100,500,1000,3000]);
	}
}