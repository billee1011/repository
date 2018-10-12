class EquipSlot extends GoodsSlot {
	public constructor() {
		super();
	}

	protected doData():void
	{
		var vo:ItemVO = this.data;
		this.label = vo.name;
	}

	public set defaultSkin(tex:egret.Texture)
	{
		this._defaultSkin.source = tex;
	}
}