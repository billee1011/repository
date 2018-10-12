class UIBase extends eui.Component
{
    /**
     * 是否当别的面板打开时 自己隐藏掉
     */
    public hideable:boolean = true;
    /**
     * 是否打开自己关掉别人
     */
    public closeOther:boolean = false;
    /**
     * 是否独自显示（后面有阴影遮挡）
     */
    public isAloneShow:boolean = false;
    /**
     * 是否居中显示
     */
    protected centerFlag:boolean = true;
    protected _eventList:Object = {};
    /**
     * 是否可以点击背景黑幕来关闭面板
     */
    public clickBackHide:boolean = false;
    /**
     */
   /* protected static thisObj:any;*/

    protected params:any;
    protected paramstype:string;
    /**
     * exml文件是否加载成功
     */
    protected isReady:boolean = false;

    private maskBg:egret.Sprite;

    /**
     * 为no不需要检测开启，否则检测功能开启
     */
    public openOrder:string = 'no';

    private groupOk:boolean = false;
    private groupName:string='';
    private skinUrl:string = '';
    /**
     * 就是检测初次打开面板有没有执行过awake，如果没有执行，资源加载完成后执行一次
     */
    private firstAwake:boolean = false;
    public constructor(skinUrl:string,groupName:string=null)
    {
        super();
        this.skinUrl = skinUrl;
        this.groupName = groupName;

        this.addEventListener(egret.Event.ADDED_TO_STAGE,this.addedToStage,this)
        this.addEventListener(egret.Event.REMOVED_FROM_STAGE,this.removeFromStage,this);

       /* var shape:egret.Shape = new egret.Shape();
        this.addChildAt(shape,0);
        shape.graphics.beginFill(0);
        shape.graphics.drawRect(0,0,480,80);
        shape.graphics.endFill();*/
 
        if(!groupName){
            this.groupOk = true;
            this.addEventListener(eui.UIEvent.COMPLETE,this.uiLoaded,this);
            this.skinName = skinUrl;
        }else
        {
            LoaderManage.instance.startLoaderGroup(groupName,this.groupLoadBack,this);
        }

    }
    /**
     * 移除舞台、移除监听
     */
    private removeFromStage(evt:egret.Event):void
    {
        var arr:Array<any>;
        for(var e in this._eventList)
        {
            arr = this._eventList[e];
            TFacade.facade.removeListener(e,arr);
        }
        this.params = this.paramstype = null;

        if(this.isAloneShow)
        {
           LayerManage.instance.hidePanelMask();
           if(this.clickBackHide){
                TFacade.facade.removeListener(ConstDefine.EMPTY_MASK_CLICK,[this.backGroundClick,this]);
           }
        }

        this.sleep();

        if(this.isAloneShow)
        {
            var f:Facade = TFacade.facade;
            if(f.currentShowUI && f.currentShowUI.parent && f.currentShowUI.isAloneShow && f.currentShowUI != this)
            {
                 LayerManage.instance.showPanelMask(f.currentShowUI.parent.getChildIndex(f.currentShowUI));
            }
        }
    }
    protected backGroundClick():void
    {
        this.hide();
    }
   /**
     *添加监听 
     */
    private addedToStage(evt:egret.Event):void
    {

        if(!this.parent){ //UI初始化了但没有被打开
            return;
        }

        var arr:Array<any>;
        for(var e in this._eventList)
        {
            arr = this._eventList[e];
            TFacade.facade.addListener(e,arr);
        }

        
        
        if(this.isAloneShow && this.isReady)
        {
           LayerManage.instance.showPanelMask(this.parent.getChildIndex(this));
           if(this.clickBackHide){
                TFacade.facade.addListener(ConstDefine.EMPTY_MASK_CLICK,[this.backGroundClick,this]);
           }
        }
        
        
        if(this.isReady){
            this.awake();
            this.firstAwake = true;
            this.layoutP();
            
        }
    }
    /**
     * 资源加载完 布局
     */
    protected layoutP():void
    {
        if(this.centerFlag)
        {
            this.x = (GlobalDefine.stageW - this.width) >> 1;
            this.y = (GlobalDefine.stageH - this.height) >> 1;
        }
    }

    private groupLoadBack(evt:any):void
    {
        if(evt.groupName == this.groupName){
            this.groupOk = true;
            LoaderManage.instance.removeListener(this.groupLoadBack,this);
            LoaderManage.instance.removeLoading();

            this.addEventListener(eui.UIEvent.COMPLETE,this.uiLoaded,this);
            this.skinName = this.skinUrl;
        }
    }

    private doubeCheck():void
    {
        if(this.groupOk && this.isReady){
            if(!this.firstAwake){
                this.bindEvent();//为啥放在这里，如果放在构造函数里，一开始先执行了addtoStage，那么如果监听到事件就会执行函数，而此时UI的各个属性还未初始化，会报错
                this.uiLoadComplete();  
                this.addedToStage(null);//这里已经执行了awake了
              //  this.awake();
                this.layoutP();
               // this.firstAwake = true;
            }
            if(this.paramstype){
                this.doExecute();
            }
        }
    }

    private uiLoaded(evt:eui.UIEvent):void
    {
        this.isReady = true;
        this.doubeCheck();
    }

    protected bindEvent():void
    {
        //override
    }    

    protected uiLoadComplete(evt:eui.UIEvent=null):void
    {
        //override
    }

    protected dataChanged(): void{ //by parent
        //override 
     }
     /**
      * type : 一般情况用不到，可随意选词 比如 fuck...等
      */
     public execute(type:string,data:any=null):void
     {
         this.params = data;
         this.paramstype = type;
         if(this.isReady){
            this.doExecute();
        }
     }

     protected doExecute():void
     {
         //override
     }

    public sleep():void
    {
        //override
    }
    public awake():void
    {
        //override
    }
    /*protected hide():void
    {

    }*/

    protected hide():void
    {
        DisplayUtil.removeDisplay(this,this.parent);
    }
}