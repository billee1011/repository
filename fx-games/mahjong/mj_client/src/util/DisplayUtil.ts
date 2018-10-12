
class DisplayUtil
{
    public constructor(){

    }
    public static removeDisplay(dis:egret.DisplayObject,parent:egret.DisplayObjectContainer=null):void
        {
            if(!dis) return;
            if(!parent){
                parent = dis.parent;
            }
            if(!parent) return;
            parent.removeChild(dis);
        }
} 
 
 