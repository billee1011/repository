class RenderBase extends eui.ItemRenderer{
	public constructor(skinUrl:string) {
		super();
		 this.addEventListener(eui.UIEvent.COMPLETE,this.uiLoadComplete,this);
        this.skinName = skinUrl;
    }
    /**选择 */
    protected _choosed:boolean;
    protected uiLoadComplete(evt:eui.UIEvent=null):void
    {
        //override
    }

    protected dataChanged(): void{
     }

     public updateXY(xx:number,yy:number):void
     {
         this.x = xx;
         this.y = yy;
     }
     public refreshRender():void
     {
         this.dataChanged();
     }

     public clear():void
     {
         
     }
     public refreshOther():void
     {
         
     }
     public get choosed():boolean
    {
        return this._choosed;
    }

    public set choosed(value:boolean)
    {
        this._choosed = value;
        this.doChoose();
    }
    
    protected doChoose():void
    {
        // override
        
    }
}