class TFacade {
	public static facade:Facade = new Facade();
	public constructor() {
		
	}
	/**
	 * type: 0:关闭    1：打开  -1 自适应打开关闭
	 * defalutLayer:默认打开到第一层面板，1、2选择
	 * name: UIName;
	 */
	public static toggleUI(name:string,type:number = -1,defalutLayer:number = 1):UIBase
	{
		var instance:UIBase = this.facade.getInstance(name);
		if(!instance)
		{
			if(type == 0){ //有才关闭
				return null;
			}
			var define:any = this.facade.getUIClass(name);
			instance = new define() as UIBase;
			this.facade.registerInstance(name,instance); 
		}
		if(instance.stage)
		{
			if(type == 0 || type == -1)
			{
				DisplayUtil.removeDisplay(instance);
			}
		}
		else
		{
			if(type == 1 || type == -1)
			{
				if(this.facade.currentShowUI && instance.closeOther == true && this.facade.currentShowUI.stage && this.facade.currentShowUI.hideable){
					DisplayUtil.removeDisplay(this.facade.currentShowUI);
				}
				if(instance.closeOther == true){
					this.facade.currentShowUI = instance;
				}
				if(defalutLayer == 1 && !instance.isAloneShow){
					LayerManage.instance.panelChildLayer1.addChild(instance);
				}else{
					LayerManage.instance.panelChildLayer2.addChild(instance);
				}
			}
		}
		return instance;
	}

	public static getProxy(name:string):any
	{
		var p:Proxy = this.facade.getProxy(name);
		if(!p){
			throw new Error("请先注册");
		}
		return p;
	}

}