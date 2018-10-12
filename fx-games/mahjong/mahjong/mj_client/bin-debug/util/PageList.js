var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var PageList = (function () {
    function PageList(list, render) {
        this.ac = new eui.ArrayCollection();
        this.list = list;
        this.list.itemRenderer = render;
    }
    PageList.prototype.addItem = function (data) {
        this.ac.addItem(data);
        this.list.dataProvider = this.ac;
    };
    PageList.prototype.displayList = function (data, centerLay, offset) {
        if (centerLay === void 0) { centerLay = 0; }
        if (offset === void 0) { offset = 0; }
        this.ac.source = data;
        this.list.dataProvider = this.ac;
        if (centerLay > 0) {
            this.centerLay = centerLay;
            this.offset = offset;
            this.outIndex = egret.setTimeout(this.layout, this, 100);
        }
    };
    PageList.prototype.layout = function () {
        egret.clearTimeout(this.outIndex);
        this.list.x = this.centerLay - this.list.contentWidth * .5 + this.offset;
    };
    /**
     * eui.list 本身就是局部刷新，可以不使用此方法，直接refreshAll
     */
    PageList.prototype.setRenderObj = function (key) {
        this.renderObj = {};
        var rb;
        for (var i = 0; i < this.list.numElements; i++) {
            rb = this.list.getElementAt(i);
            if (rb) {
                var data = rb.data;
                this.renderObj[data[key]] = rb;
            }
        }
    };
    PageList.prototype.updateChange = function () {
        this.list.dataProvider = this.ac;
    };
    /*public addItemToAC(data:any):void
    {
        this.ac.addItem(data);
        //this.list.dataProvider = this.ac;
    }*/
    /**
     * eui.list 本身就是局部刷新，可以不使用此方法，直接refreshAll
     */
    PageList.prototype.singleRefresh = function (key) {
        var rb = this.renderObj[key];
        if (rb) {
            rb.refreshRender();
        }
    };
    PageList.prototype.refreshAll = function () {
        var rb;
        for (var i = 0; i < this.list.numElements; i++) {
            rb = this.list.getElementAt(i);
            if (rb) {
                rb.refreshRender();
            }
        }
    };
    return PageList;
}());
__reflect(PageList.prototype, "PageList");
//# sourceMappingURL=PageList.js.map