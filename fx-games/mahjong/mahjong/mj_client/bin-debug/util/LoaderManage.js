var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var LoaderManage = (function () {
    //private listerObj:Object;
    function LoaderManage() {
        //this.listerObj = {};
        this.loadUI = new LoadingUI();
        RES.addEventListener(RES.ResourceEvent.GROUP_LOAD_ERROR, this.onResourceLoadError, this);
        RES.addEventListener(RES.ResourceEvent.GROUP_PROGRESS, this.onResourceProgress, this);
        RES.addEventListener(RES.ResourceEvent.ITEM_LOAD_ERROR, this.onItemLoadError, this);
    }
    Object.defineProperty(LoaderManage, "instance", {
        get: function () {
            if (!this._instance) {
                this._instance = new LoaderManage();
            }
            return this._instance;
        },
        enumerable: true,
        configurable: true
    });
    LoaderManage.prototype.startLoaderGroup = function (source, backFun, thisObj) {
        if (RES.isGroupLoaded(source)) {
            this.removeListener(backFun, thisObj);
            var temp = { 'groupName': source };
            backFun.call(thisObj, temp);
        }
        else {
            LayerManage.instance.tipLayer.addChild(this.loadUI);
            RES.addEventListener(RES.ResourceEvent.GROUP_COMPLETE, backFun, thisObj);
            RES.loadGroup(source);
        }
    };
    LoaderManage.prototype.removeListener = function (backFun, thisObj) {
        RES.removeEventListener(RES.ResourceEvent.GROUP_COMPLETE, backFun, thisObj);
    };
    LoaderManage.prototype.removeLoading = function () {
        DisplayUtil.removeDisplay(this.loadUI);
    };
    /**
    * 资源组加载出错
    *  The resource group loading failed
    */
    LoaderManage.prototype.onItemLoadError = function (event) {
        console.warn("Url:" + event.resItem.url + " has failed to load");
    };
    /**
     * 资源组加载出错
     * Resource group loading failed
     */
    LoaderManage.prototype.onResourceLoadError = function (event) {
        //TODO
        console.warn("Group:" + event.groupName + " has failed to load");
        //忽略加载失败的项目
        //ignore loading failed projects
        // this.onResourceLoadComplete(event);
    };
    /**
     * preload资源组加载进度
     * loading process of preload resource
     */
    LoaderManage.prototype.onResourceProgress = function (event) {
        if (this.loadUI) {
            this.loadUI.setProgress(event.itemsLoaded, event.itemsTotal);
        }
    };
    return LoaderManage;
}());
__reflect(LoaderManage.prototype, "LoaderManage");
//# sourceMappingURL=LoaderManage.js.map