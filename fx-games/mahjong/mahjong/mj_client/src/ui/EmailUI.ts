class EmailUI extends UIBase{
	public static NAME:string = 'EmailUI';
	public constructor() {
		super('resource/UI_exml/Email.exml');
		this.isAloneShow = true;
	}
	protected uiLoadComplete():void
	{

	}
}