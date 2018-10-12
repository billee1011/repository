var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var State = (function (_super) {
    __extends(State, _super);
    function State() {
        var _this = _super.call(this) || this;
        _this.obj = {};
        return _this;
    }
    State.prototype.add = function (type, elements) {
        this.obj[type] = elements;
    };
    Object.defineProperty(State.prototype, "state", {
        get: function () {
            return this._state;
        },
        set: function (type) {
            this._state = type;
            var arr = this.obj[type];
            if (!arr)
                return;
            this.switchDisplay(true, arr);
            for (var key in this.obj) {
                if (key != type) {
                    arr = this.obj[key];
                    this.switchDisplay(false, arr);
                }
            }
        },
        enumerable: true,
        configurable: true
    });
    State.prototype.switchDisplay = function (show, arr) {
        var dis;
        for (var key in arr) {
            dis = arr[key];
            if (show && dis.stage == null) {
                this.addChild(dis);
            }
            else if (!show && dis.parent) {
                DisplayUtil.removeDisplay(dis, dis.parent);
            }
        }
    };
    return State;
}(egret.DisplayObjectContainer));
__reflect(State.prototype, "State");
//# sourceMappingURL=State.js.map