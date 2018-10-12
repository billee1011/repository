var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var EnterRoomUI = (function (_super) {
    __extends(EnterRoomUI, _super);
    function EnterRoomUI() {
        var _this = _super.call(this, 'resource/UI_exml/EnterRoom.exml', 'enterRoom') || this;
        _this.isAloneShow = true;
        _this.closeOther = false;
        return _this;
    }
    EnterRoomUI.prototype.bindEvent = function () {
        this.model = TFacade.getProxy(MjModel.NAME);
    };
    EnterRoomUI.prototype.uiLoadComplete = function () {
        var img;
        img = new eui.Image;
        img.source = PathDefine.UI_IMAGE + 'frameBarShort.png';
        this.addChild(img);
        img.x = 150;
        img.y = 15;
        img = new eui.Image();
        img.source = this.getTexture('enterRoomTitle');
        this.addChild(img);
        img.x = 274;
        img.y = 19;
        var i = 0;
        for (i = 1; i <= 12; i++) {
            img = new eui.Image();
            if (i <= 9) {
                img.source = this.getTexture('btn_num_' + i);
                img.name = i.toString();
            }
            else if (i == 10) {
                img.source = this.getTexture('btn_reset');
                img.name = '100'; //重置
            }
            else if (i == 11) {
                img.source = this.getTexture('btn_num_0');
                img.name = '0';
            }
            else {
                img.source = this.getTexture('btn_del');
                img.name = '90'; //删除
            }
            img.x = (i - 1) % 3 * 193 + 66;
            img.y = Math.floor((i - 1) / 3) * 65 + 159;
            this.addChild(img);
            img.addEventListener(egret.TouchEvent.TOUCH_TAP, this.operateHandler, this);
        }
        this.numDatas = [];
        this.btn_close.addEventListener(egret.TouchEvent.TOUCH_TAP, this.hide, this);
    };
    EnterRoomUI.prototype.operateHandler = function (evt) {
        var img = evt.currentTarget;
        var value = parseInt(img.name);
        if (value >= 0 && value <= 9) {
            if (this.numDatas.length >= 6) {
                return;
            }
            this.numDatas.push(value);
        }
        else if (value == 90) {
            this.numDatas.pop();
        }
        else if (value == 100) {
            this.numDatas.length = 0;
        }
        else {
            return;
        }
        this.updateLabel();
    };
    EnterRoomUI.prototype.updateLabel = function () {
        var label;
        var value = this.numDatas.join('');
        this.label_num.text = value;
        if (this.numDatas.length >= 6) {
            this.checkPassword(value);
        }
    };
    EnterRoomUI.prototype.checkPassword = function (v) {
        var me = parseInt(v);
        SendOperate.instance.requestEnterRoom(me);
        //this.hide();
    };
    EnterRoomUI.prototype.getTexture = function (name) {
        return SheetManage.getTextureFromSheet(name, 'mj_enterroom_json');
    };
    EnterRoomUI.prototype.sleep = function () {
        this.numDatas.length = 0;
        this.updateLabel();
    };
    return EnterRoomUI;
}(UIBase));
EnterRoomUI.NAME = 'EnterRoomUI';
__reflect(EnterRoomUI.prototype, "EnterRoomUI");
//# sourceMappingURL=EnterRoomUI.js.map