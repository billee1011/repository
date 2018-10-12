class SheetManage {
	public constructor() {
	}

	   /**
     * 根据name关键字创建一个Bitmap对象。此name 是根据TexturePacker 组合成的一张位图
     */
    public static createBitmapFromSheet(name:string, sheetName:string):egret.Bitmap {
        var sheet:egret.SpriteSheet = RES.getRes(sheetName);
        var texture:egret.Texture = sheet.getTexture(name);
        var result:egret.Bitmap = new egret.Bitmap();
        result.texture = texture;
        return result;
    }
    /**
     * sheetName 组名
     */
    public static getTextureFromSheet(name:string, sheetName:string):egret.Texture {
        var sheet:egret.SpriteSheet = RES.getRes(sheetName);
        var result:egret.Texture = sheet.getTexture(name);
        return result;
    }

    public static getTextureFromCenter(name:string):egret.Texture
    {
        return SheetManage.getTextureFromSheet(name,'mj_center_json');
    }
    
    public static getTextureFromOperate(name:string):egret.Texture
    {
        return SheetManage.getTextureFromSheet(name,'mj_operate_json');
    }

}