var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var BagCheckLabel = (function (_super) {
    __extends(BagCheckLabel, _super);
    function BagCheckLabel(label, align) {
        if (align === void 0) { align = 'left'; }
        var _this = _super.call(this) || this;
        if (!label) {
            _this.label = new eui.Label();
            _this.label.size = 16;
            _this.label.fontFamily = '微软雅黑';
            _this.label.textAlign = align;
        }
        else {
            _this.label = label;
        }
        _this.label.x = 0;
        _this.label.y = 0;
        _this.addChild(_this.label);
        _this.addEventListener(egret.Event.ADDED_TO_STAGE, _this.toStageHandler, _this);
        _this.addEventListener(egret.Event.REMOVED_FROM_STAGE, _this.leaveStageHandler, _this);
        return _this;
    }
    BagCheckLabel.prototype.updateXY = function (xx, yy) {
        this.x = xx;
        this.y = yy;
    };
    BagCheckLabel.prototype.toStageHandler = function (evt) {
    };
    BagCheckLabel.prototype.leaveStageHandler = function (evt) {
    };
    BagCheckLabel.prototype.update = function () {
        if (!this.id) {
            return;
        }
        // var gproxy:GoodsProxy = TFacade.getProxy(GoodsProxy.NAME);
        // var have:number = gproxy.getCountByItemId(this.id);
        // this.label.text = `${have}/${this.aim}`;
        // this.label.textColor = have >= this.aim ? 0x00FF00 : 0xFF0000;
    };
    BagCheckLabel.prototype.setData = function (id, aimCount) {
        this.id = id;
        this.aim = aimCount;
        this.update();
    };
    return BagCheckLabel;
}(egret.DisplayObjectContainer));
__reflect(BagCheckLabel.prototype, "BagCheckLabel");
//# sourceMappingURL=BagCheckLabel.js.map