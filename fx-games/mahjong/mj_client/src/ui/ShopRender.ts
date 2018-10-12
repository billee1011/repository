class ShopRender extends RenderBase{
	public constructor() {
		super('resource/UI_exml/ShopItem.exml');
	}
	private label_diamond:eui.Label;
	private btn_buy:eui.Button;
	protected uiLoadComplete():void
	{

	}

	protected dataChanged():void
	{
		let v:number = this.data;
		this.label_diamond.text = v.toString();
		this.btn_buy.label = v.toString();
	}
}