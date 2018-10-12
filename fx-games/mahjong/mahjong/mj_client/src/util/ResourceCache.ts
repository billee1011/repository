/**
 * 资源缓存
 */
class ResourceCache {
	public imageCache:Object;
	public mcCache:Object;
	public constructor() {
		this.imageCache = {};
		this.mcCache = {};
	}
	public static _instance:ResourceCache;
	public static get instance():ResourceCache
	{
		if(!this._instance){
			this._instance = new ResourceCache();
		}
		return this._instance;
	}

	public addImageToCache(key:string,data:any):void
	{
		this.imageCache[key] = data;
	}
	public getImageFromCache(key:string):any
	{
		return this.imageCache[key];
	}
	public addMcToCache(key:string,data:any):void
	{
		this.mcCache[key] = data;
	}
	public getMcFromCache(key:string):any
	{
		return this.mcCache[key];
	}
}