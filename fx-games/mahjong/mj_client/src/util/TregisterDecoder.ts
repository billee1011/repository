class TregisterDecoder {
	public constructor() {
		var facade:Facade = TFacade.facade;
		
		//new SceneDecoder().init(facade);
		new GameDecoder().init(facade);
		new FrameDecoder().init(facade);
	}

}