class HasNameSlot extends GoodsSlot{
	public constructor() {
		super();
		this.tipable = true;
	}
	protected doData():void
	{
		var vo:ItemVO = this.data;
		this.label = vo.name;
	}
}