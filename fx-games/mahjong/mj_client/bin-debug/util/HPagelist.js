var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var HPagelist = (function (_super) {
    __extends(HPagelist, _super);
    function HPagelist(render, renderWidth, renderHeight, hor, horNum) {
        if (hor === void 0) { hor = true; }
        if (horNum === void 0) { horNum = 1; }
        var _this = _super.call(this) || this;
        /**
         * -1 为反向排列(hor=false时使用较好)
         */
        _this.layoutDirX = 1;
        /**
         * -1 为反向排列(hor=true时使用较好)
         */
        _this.layoutDirY = 1;
        _this.gap = 0;
        _this.touchEnabled = true;
        _this.cls = render;
        _this.rwidth = renderWidth;
        _this.rheight = renderHeight;
        _this.isHor = hor;
        _this.horNum = horNum;
        return _this;
    }
    HPagelist.prototype.displayList = function (data) {
        this.len = data ? data.length : 0;
        var render;
        for (var key in this.dataList) {
            render = this.dataList[key];
            //render.removeAllEvent();
            render.removeEventListener(egret.TouchEvent.TOUCH_TAP, this.renderClickHandler, this);
            render.clear();
            DisplayUtil.removeDisplay(render);
            CachePool.getInstance().reBack(render);
        }
        if (this.dataList) {
            this.dataList.length = 0;
        }
        else {
            this.dataList = [];
        }
        for (var i = 0; i < this.len; i++) {
            var value = data[i];
            render = CachePool.getInstance().getObject(this.cls);
            render.data = value;
            this.arrange(render, i);
            render.addEventListener(egret.TouchEvent.TOUCH_TAP, this.renderClickHandler, this);
            this.dataList.push(render);
        }
    };
    HPagelist.prototype.getAllItem = function () {
        return this.dataList;
    };
    HPagelist.prototype.refreshAll = function () {
        var render;
        for (var key in this.dataList) {
            render = this.dataList[key];
            render.refreshRender();
        }
    };
    HPagelist.prototype.refreshOther = function () {
        var render;
        for (var key in this.dataList) {
            render = this.dataList[key];
            render.refreshOther();
        }
    };
    HPagelist.prototype.renderClickHandler = function (event) {
        var render = event.currentTarget;
        this.selectItem(render);
    };
    HPagelist.prototype.selectItem = function (render) {
        this.currentItem = render;
        render.choosed = true;
        this.simpleEvent(render);
        var temp;
        for (var key in this.dataList) {
            temp = this.dataList[key];
            if (temp != render && temp.choosed) {
                temp.choosed = false;
                break;
            }
        }
    };
    /**
     *派发事件
        * @param render
        *
        */
    HPagelist.prototype.simpleEvent = function (render) {
        //EventControl.dispatchEvent(HPagelist.RENDER_CHANGE,obj);
        this.dispatchEvent(new CurEvent(HPagelist.RENDER_CHANGE, render, render.data));
    };
    HPagelist.prototype.selectAt = function (index) {
        var render = this.dataList[index];
        this.selectItem(render);
    };
    /*public selectItem(render:RenderBase):void
    {
        render.selected = true;
        this.simpleEvent(render);
        var temp:RenderBase;
        for (let key in this.dataList)
        {
            temp = this.dataList[key];
            if(temp != render && temp.selected){
                temp.selected = false;
                break;
            }
        }
    }	*/
    HPagelist.prototype.arrange = function (dis, i) {
        if (this.downUp) {
            this.addChildAt(dis, 0);
        }
        else
            this.addChild(dis);
        var a = Math.floor(i % this.horNum);
        var b = Math.floor(i / this.horNum);
        if (this.isHor) {
            if (this.rightLeft) {
                a = this.horNum - a;
            }
            dis.x = a * (this.rwidth + this.gap) * this.layoutDirX;
            dis.y = b * (this.rheight + this.gap) * this.layoutDirY;
            if (this.layoutDirY == -1) {
                this.setChildIndex(dis, a);
            }
        }
        else {
            dis.x = b * (this.rwidth + this.gap) * this.layoutDirX;
            if (this.downUp) {
                a = this.horNum - a;
            }
            dis.y = a * (this.rheight + this.gap) * this.layoutDirY;
        }
    };
    HPagelist.prototype.updateXY = function (x, y) {
        this.x = x;
        this.y = y;
    };
    return HPagelist;
}(egret.Sprite));
HPagelist.RENDER_CHANGE = 'pagelist_render_change';
__reflect(HPagelist.prototype, "HPagelist");
//# sourceMappingURL=HPagelist.js.map