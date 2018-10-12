//////////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (c) 2014-present, Egret Technology.
//  All rights reserved.
//  Redistribution and use in source and binary forms, with or without
//  modification, are permitted provided that the following conditions are met:
//
//     * Redistributions of source code must retain the above copyright
//       notice, this list of conditions and the following disclaimer.
//     * Redistributions in binary form must reproduce the above copyright
//       notice, this list of conditions and the following disclaimer in the
//       documentation and/or other materials provided with the distribution.
//     * Neither the name of the Egret nor the
//       names of its contributors may be used to endorse or promote products
//       derived from this software without specific prior written permission.
//
//  THIS SOFTWARE IS PROVIDED BY EGRET AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
//  OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
//  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
//  IN NO EVENT SHALL EGRET AND CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
//  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;LOSS OF USE, DATA,
//  OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
//  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
//  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
//  EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//
//////////////////////////////////////////////////////////////////////////////////////
var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var LoadingUI = (function (_super) {
    __extends(LoadingUI, _super);
    function LoadingUI() {
        var _this = _super.call(this) || this;
        _this.createView();
        return _this;
    }
    LoadingUI.prototype.chageSkin = function (texture) {
        this._loadingRun = new egret.Bitmap(texture);
        this.addChild(this._loadingRun);
        this._loadingRun.anchorOffsetX = this._loadingRun.width * .5;
        this._loadingRun.anchorOffsetY = this._loadingRun.height * .5;
        this._loadingRun.x = GlobalDefine.stage.stageWidth * .5;
        this._loadingRun.y = GlobalDefine.stage.stageHeight * .5;
        this._txProgress = new egret.TextField;
        this._txProgress.textAlign = egret.HorizontalAlign.CENTER;
        this._txProgress.verticalAlign = egret.VerticalAlign.MIDDLE;
        this._txProgress.x = GlobalDefine.stage.stageWidth * .5 - 100;
        this._txProgress.width = 200;
        this._txProgress.y = GlobalDefine.stage.stageHeight * .5 - 100;
        this._txProgress.height = 200;
        this._txProgress.size = 16;
        // this._txProgress.stroke = 1;
        // this._txProgress.strokeColor = 0;
        this.addChild(this._txProgress);
        //this.removeChild(this.textField);
        //this.textField = null;
        this.addEventListener(egret.Event.REMOVED_FROM_STAGE, this.removeToStage, this);
        this.addEventListener(egret.Event.ADDED_TO_STAGE, this.addtoStageHandler, this);
        //this.addtoStage(null);
    };
    LoadingUI.prototype.addtoStageHandler = function (evt) {
        this.addEventListener(egret.Event.ENTER_FRAME, this.runLoading, this);
    };
    LoadingUI.prototype.removeToStage = function (evt) {
        if (this.hasEventListener(egret.Event.ENTER_FRAME)) {
            this.removeEventListener(egret.Event.ENTER_FRAME, this.runLoading, this);
        }
    };
    LoadingUI.prototype.runLoading = function (evt) {
        this._loadingRun.rotation += 3;
    };
    LoadingUI.prototype.createView = function () {
        /* this.textField = new egret.TextField();
         this.addChild(this.textField);
         this.textField.y = 300;
         this.textField.width = 480;
         this.textField.height = 100;
         this.textField.textAlign = "center";*/
    };
    LoadingUI.prototype.setProgress = function (current, total) {
        if (this._txProgress) {
            this._txProgress.text = Math.round(current / total * 100) + "%";
        }
        else {
        }
    };
    return LoadingUI;
}(egret.Sprite));
__reflect(LoadingUI.prototype, "LoadingUI");
//# sourceMappingURL=LoadingUI.js.map