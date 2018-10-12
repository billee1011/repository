class EventControl {
	private static dispatcher:egret.EventDispatcher=new egret.EventDispatcher();
	public static BODY_LOADED="bodyimageload";
	public static WEAPON_LOADED="weapon_loaded";
	public static WING_LOADED="wing_loaded";
	public static HEAD_LOADED="head_loaded";
	public static SHADOW_LOADED='shadow_loaded';
	public static Map_Image_Loaded="mapimageloaded";
	public static Small_MapImg_Loaded="smallmapimgloaded";
	public constructor() {
	}

/*	public static dispatchEvent(type:string,data:any)
	{
		var event:CurEvent=new CurEvent(type,data);
	//	event.data=data;
		this.dispatcher.dispatchEvent(event);
	}
*/
	public static addEventListener(type:string,listener:Function,thisobject:any)
	{
		this.dispatcher.addEventListener(type,listener,thisobject);
	}

		public static removeEventListener(type:string,listener:Function,thisobject:any)
	{
		this.dispatcher.removeEventListener(type,listener,thisobject);
	}
}