var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var TabItemList = (function (_super) {
    __extends(TabItemList, _super);
    function TabItemList() {
        var _this = _super.call(this) || this;
        _this.tabArr = [];
        return _this;
    }
    TabItemList.prototype.addItem = function (ti) {
        this.tabArr.push(ti);
        this.addChild(ti.skin);
        this.addEventListener(egret.TouchEvent.TOUCH_TAP, this.touchHandler, this);
    };
    TabItemList.prototype.setSelectedIcon = function (tex, offx, offy) {
        if (offx === void 0) { offx = 0; }
        if (offy === void 0) { offy = 0; }
        this._selectedIcon = new eui.Image();
        this._selectedIcon.source = tex;
        this.addChild(this._selectedIcon);
        this.setChildIndex(this._selectedIcon, 0);
        this._offx = offx;
        this._offy = offy;
    };
    TabItemList.prototype.touchHandler = function (evt) {
        var ti = evt.target;
        if (!ti) {
            return;
        }
        var item;
        for (var key in this.tabArr) {
            var element = this.tabArr[key];
            if (element.skin != ti) {
                element.selected = false;
            }
            else {
                item = element;
            }
        }
        this.updateSelectIcon(item);
        item.selected = true;
        TFacade.facade.simpleDispatcher(TabItemList.TAB_CHANGE, item);
    };
    Object.defineProperty(TabItemList.prototype, "selectItem", {
        get: function () {
            for (var key in this.tabArr) {
                var element = this.tabArr[key];
                if (element.selected) {
                    return element;
                }
            }
            return null;
        },
        set: function (tb) {
            tb.selected = true;
            for (var key in this.tabArr) {
                var element = this.tabArr[key];
                if (element != tb) {
                    element.selected = false;
                }
            }
            this.updateSelectIcon(tb);
            //EventControl.dispatchEvent(TabItemList.TAB_CHANGE,tb);
            TFacade.facade.simpleDispatcher(TabItemList.TAB_CHANGE, tb);
        },
        enumerable: true,
        configurable: true
    });
    TabItemList.prototype.updateSelectIcon = function (tb) {
        if (this._selectedIcon) {
            this._selectedIcon.x = tb.skin.x + this._offx;
            this._selectedIcon.y = tb.skin.y + this._offy;
        }
    };
    TabItemList.prototype.selectAt = function (index) {
        var item = this.tabArr[index];
        this.selectItem = item;
    };
    return TabItemList;
}(egret.Sprite));
TabItemList.TAB_CHANGE = 'TABLIST_TAB_CHANGE';
__reflect(TabItemList.prototype, "TabItemList");
//# sourceMappingURL=TabItemList.js.map