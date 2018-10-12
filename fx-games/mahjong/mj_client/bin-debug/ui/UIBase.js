var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var UIBase = (function (_super) {
    __extends(UIBase, _super);
    function UIBase(skinUrl, groupName) {
        if (groupName === void 0) { groupName = null; }
        var _this = _super.call(this) || this;
        /**
         * 是否当别的面板打开时 自己隐藏掉
         */
        _this.hideable = true;
        /**
         * 是否打开自己关掉别人
         */
        _this.closeOther = false;
        /**
         * 是否独自显示（后面有阴影遮挡）
         */
        _this.isAloneShow = false;
        /**
         * 是否居中显示
         */
        _this.centerFlag = true;
        _this._eventList = {};
        /**
         * 是否可以点击背景黑幕来关闭面板
         */
        _this.clickBackHide = false;
        /**
         * exml文件是否加载成功
         */
        _this.isReady = false;
        /**
         * 为no不需要检测开启，否则检测功能开启
         */
        _this.openOrder = 'no';
        _this.groupOk = false;
        _this.groupName = '';
        _this.skinUrl = '';
        /**
         * 就是检测初次打开面板有没有执行过awake，如果没有执行，资源加载完成后执行一次
         */
        _this.firstAwake = false;
        _this.skinUrl = skinUrl;
        _this.groupName = groupName;
        _this.addEventListener(egret.Event.ADDED_TO_STAGE, _this.addedToStage, _this);
        _this.addEventListener(egret.Event.REMOVED_FROM_STAGE, _this.removeFromStage, _this);
        /* var shape:egret.Shape = new egret.Shape();
         this.addChildAt(shape,0);
         shape.graphics.beginFill(0);
         shape.graphics.drawRect(0,0,480,80);
         shape.graphics.endFill();*/
        if (!groupName) {
            _this.groupOk = true;
            _this.addEventListener(eui.UIEvent.COMPLETE, _this.uiLoaded, _this);
            _this.skinName = skinUrl;
        }
        else {
            LoaderManage.instance.startLoaderGroup(groupName, _this.groupLoadBack, _this);
        }
        return _this;
    }
    /**
     * 移除舞台、移除监听
     */
    UIBase.prototype.removeFromStage = function (evt) {
        var arr;
        for (var e in this._eventList) {
            arr = this._eventList[e];
            TFacade.facade.removeListener(e, arr);
        }
        this.params = this.paramstype = null;
        if (this.isAloneShow) {
            LayerManage.instance.hidePanelMask();
            if (this.clickBackHide) {
                TFacade.facade.removeListener(ConstDefine.EMPTY_MASK_CLICK, [this.backGroundClick, this]);
            }
        }
        this.sleep();
        if (this.isAloneShow) {
            var f = TFacade.facade;
            if (f.currentShowUI && f.currentShowUI.parent && f.currentShowUI.isAloneShow && f.currentShowUI != this) {
                LayerManage.instance.showPanelMask(f.currentShowUI.parent.getChildIndex(f.currentShowUI));
            }
        }
    };
    UIBase.prototype.backGroundClick = function () {
        this.hide();
    };
    /**
      *添加监听
      */
    UIBase.prototype.addedToStage = function (evt) {
        if (!this.parent) {
            return;
        }
        var arr;
        for (var e in this._eventList) {
            arr = this._eventList[e];
            TFacade.facade.addListener(e, arr);
        }
        if (this.isAloneShow && this.isReady) {
            LayerManage.instance.showPanelMask(this.parent.getChildIndex(this));
            if (this.clickBackHide) {
                TFacade.facade.addListener(ConstDefine.EMPTY_MASK_CLICK, [this.backGroundClick, this]);
            }
        }
        if (this.isReady) {
            this.awake();
            this.firstAwake = true;
            this.layoutP();
        }
    };
    /**
     * 资源加载完 布局
     */
    UIBase.prototype.layoutP = function () {
        if (this.centerFlag) {
            this.x = (GlobalDefine.stageW - this.width) >> 1;
            this.y = (GlobalDefine.stageH - this.height) >> 1;
        }
    };
    UIBase.prototype.groupLoadBack = function (evt) {
        if (evt.groupName == this.groupName) {
            this.groupOk = true;
            LoaderManage.instance.removeListener(this.groupLoadBack, this);
            LoaderManage.instance.removeLoading();
            this.addEventListener(eui.UIEvent.COMPLETE, this.uiLoaded, this);
            this.skinName = this.skinUrl;
        }
    };
    UIBase.prototype.doubeCheck = function () {
        if (this.groupOk && this.isReady) {
            if (!this.firstAwake) {
                this.bindEvent(); //为啥放在这里，如果放在构造函数里，一开始先执行了addtoStage，那么如果监听到事件就会执行函数，而此时UI的各个属性还未初始化，会报错
                this.uiLoadComplete();
                this.addedToStage(null); //这里已经执行了awake了
                //  this.awake();
                this.layoutP();
            }
            if (this.paramstype) {
                this.doExecute();
            }
        }
    };
    UIBase.prototype.uiLoaded = function (evt) {
        this.isReady = true;
        this.doubeCheck();
    };
    UIBase.prototype.bindEvent = function () {
        //override
    };
    UIBase.prototype.uiLoadComplete = function (evt) {
        if (evt === void 0) { evt = null; }
        //override
    };
    UIBase.prototype.dataChanged = function () {
        //override 
    };
    /**
     * type : 一般情况用不到，可随意选词 比如 fuck...等
     */
    UIBase.prototype.execute = function (type, data) {
        if (data === void 0) { data = null; }
        this.params = data;
        this.paramstype = type;
        if (this.isReady) {
            this.doExecute();
        }
    };
    UIBase.prototype.doExecute = function () {
        //override
    };
    UIBase.prototype.sleep = function () {
        //override
    };
    UIBase.prototype.awake = function () {
        //override
    };
    /*protected hide():void
    {

    }*/
    UIBase.prototype.hide = function () {
        DisplayUtil.removeDisplay(this, this.parent);
    };
    return UIBase;
}(eui.Component));
__reflect(UIBase.prototype, "UIBase");
//# sourceMappingURL=UIBase.js.map