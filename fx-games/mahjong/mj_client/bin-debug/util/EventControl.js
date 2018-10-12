var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var EventControl = (function () {
    function EventControl() {
    }
    /*	public static dispatchEvent(type:string,data:any)
        {
            var event:CurEvent=new CurEvent(type,data);
        //	event.data=data;
            this.dispatcher.dispatchEvent(event);
        }
    */
    EventControl.addEventListener = function (type, listener, thisobject) {
        this.dispatcher.addEventListener(type, listener, thisobject);
    };
    EventControl.removeEventListener = function (type, listener, thisobject) {
        this.dispatcher.removeEventListener(type, listener, thisobject);
    };
    return EventControl;
}());
EventControl.dispatcher = new egret.EventDispatcher();
EventControl.BODY_LOADED = "bodyimageload";
EventControl.WEAPON_LOADED = "weapon_loaded";
EventControl.WING_LOADED = "wing_loaded";
EventControl.HEAD_LOADED = "head_loaded";
EventControl.SHADOW_LOADED = 'shadow_loaded';
EventControl.Map_Image_Loaded = "mapimageloaded";
EventControl.Small_MapImg_Loaded = "smallmapimgloaded";
__reflect(EventControl.prototype, "EventControl");
//# sourceMappingURL=EventControl.js.map