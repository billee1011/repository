package com.cai.ai;

public class AiWrap {
	// 是否是超出出牌，给他托管
   private boolean isNeedTrustee;
   private long delayTime;
   private long startTime;
   private long maxTrusteeTime;
   
   public AiWrap(long delayTime){
	   this(false, delayTime);
   }
		   
	public AiWrap(boolean isNeedTrustee, long delayTime){
		this.isNeedTrustee = isNeedTrustee;
		this.delayTime = delayTime;
		this.startTime = System.currentTimeMillis();
	}
	
	public void setMaxTrusteeTime(long maxTrusteeTime) {
		this.maxTrusteeTime = maxTrusteeTime;
	}

	public boolean isNeedTrustee() {
		return isNeedTrustee;
	}
	
	public long getDelayTime() {
		return delayTime;
	}
	
	public AiWrap getNextAiWrap(){
		long leftTime = maxTrusteeTime - (System.currentTimeMillis() - startTime);
		if(leftTime < 0){
			leftTime = 0;
		}
		AiWrap wrap = new AiWrap(true, leftTime);
		return wrap;
	}
	
}
