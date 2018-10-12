var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var HelpUI = (function (_super) {
    __extends(HelpUI, _super);
    function HelpUI() {
        var _this = _super.call(this, 'resource/UI_exml/Help.exml') || this;
        _this.centerFlag = true;
        _this.isAloneShow = true;
        _this.closeOther = false;
        return _this;
    }
    HelpUI.prototype.uiLoadComplete = function () {
        this.sb_target = new ScrollBar(this.list_target);
        this.addChild(this.sb_target);
        this.plist = new PageList(this.list_target, HelpRender);
        this.plist.displayList(['啊发顺丰福建省付款as发骚发骚发骚撒发顺丰发杀神风啊啊算法沙发上发as发生发发啊发顺丰福建省付款as发骚发骚发骚撒发顺丰发杀神风啊啊算法沙发上发as发生发发啊发顺丰福建省付款as发骚发骚发骚撒发顺丰发杀神风啊啊算法沙发上发as发生发发啊发顺丰福建省付款as发骚发骚发骚撒发顺丰发杀神风啊啊算法沙发上发as发生发发啊发顺丰福建省付款as发骚发骚发骚撒发顺丰发杀神风啊啊算法沙发上发as发生发发啊发顺丰福建省付款as发骚发骚发骚撒发顺丰发杀神风啊啊算法沙发上发as发生发发啊发顺丰福建省付款as发骚发骚发骚撒发顺丰发杀神风啊啊算法沙发上发as发生发发啊发顺丰福建省付款as发骚发骚发骚撒发顺丰发杀神风啊啊算法沙发上发as发生发发啊发顺丰福建省付款as发骚发骚发骚撒发顺丰发杀神风啊啊算法沙发上发as发生发发啊发顺丰福建省付款as发骚发骚发骚撒发顺丰发杀神风啊啊算法沙发上发as发生发发啊发顺丰福建省付款as发骚发骚发骚撒发顺丰发杀神风啊啊算法沙发上发as发生发发']);
    };
    return HelpUI;
}(UIBase));
HelpUI.NAME = 'HelpUI';
__reflect(HelpUI.prototype, "HelpUI");
//# sourceMappingURL=HelpUI.js.map