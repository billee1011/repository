class LoaderManage {
	private static _instance:LoaderManage;
	public loadUI:LoadingUI;
	//private listerObj:Object;
	public constructor() {
		//this.listerObj = {};
        this.loadUI = new LoadingUI();
        RES.addEventListener(RES.ResourceEvent.GROUP_LOAD_ERROR, this.onResourceLoadError, this);
        RES.addEventListener(RES.ResourceEvent.GROUP_PROGRESS, this.onResourceProgress, this);
        RES.addEventListener(RES.ResourceEvent.ITEM_LOAD_ERROR, this.onItemLoadError, this);
	}

	public static get instance():LoaderManage
	{
		if(!this._instance){
			this._instance = new LoaderManage();
		}
		return this._instance;
	}




	public startLoaderGroup(source:string,backFun:(event:egret.Event)=>void,thisObj:any):void
	{
        if(RES.isGroupLoaded(source)){
            this.removeListener(backFun,thisObj);
            var temp:Object = {'groupName':source};
            backFun.call(thisObj,temp);
        }
        else{
            LayerManage.instance.tipLayer.addChild(this.loadUI);
            RES.addEventListener(RES.ResourceEvent.GROUP_COMPLETE, backFun, thisObj);
            RES.loadGroup(source);
        }
	}

    public removeListener(backFun:(event:egret.Event)=>void,thisObj:any):void
    {
       RES.removeEventListener(RES.ResourceEvent.GROUP_COMPLETE,backFun,thisObj);
    }

    public removeLoading():void
    {
        DisplayUtil.removeDisplay(this.loadUI);
    }

	 /**
     * 资源组加载出错
     *  The resource group loading failed
     */
    private onItemLoadError(event:RES.ResourceEvent):void {
        console.warn("Url:" + event.resItem.url + " has failed to load");
    }
    /**
     * 资源组加载出错
     * Resource group loading failed
     */
    private onResourceLoadError(event:RES.ResourceEvent):void {
        //TODO
        console.warn("Group:" + event.groupName + " has failed to load");
        //忽略加载失败的项目
        //ignore loading failed projects
       // this.onResourceLoadComplete(event);
    }
    /**
     * preload资源组加载进度
     * loading process of preload resource
     */
    private onResourceProgress(event:RES.ResourceEvent):void {
        if (this.loadUI) {
           this.loadUI.setProgress(event.itemsLoaded, event.itemsTotal);
           // egret.log(`${event.itemsLoaded}/${event.itemsTotal}`);
           // Math.round( itemsLoaded / itemsTotal * 100 ) + "%";
           /*if(event.itemsLoaded >= event.itemsTotal){
             
           }*/
        }
    }
}