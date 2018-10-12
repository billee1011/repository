class GonggaoUI extends UIBase{
	public static NAME:string = 'GonggaoUI';
	public constructor() {
		super('resource/UI_exml/Gonggao.exml');

		this.centerFlag = true;
		this.isAloneShow = true;
		this.closeOther = false;
	}
	protected uiLoadComplete():void
	{

	}
}