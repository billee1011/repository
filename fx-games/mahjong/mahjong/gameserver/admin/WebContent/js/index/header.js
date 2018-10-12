;(function(){if(top!=window){var insertStyle=function(rules){var node=document.createElement("style");node.type='text/css';document.getElementsByTagName("head")[0].appendChild(node);if(rules){if(node.styleSheet){node.styleSheet.cssText=rules;}else{node.appendChild(document.createTextNode(rules));}}
return node.sheet||node;}
var insertJS=function(src,hdl){var head=document.getElementsByTagName("head")[0];var script=document.createElement("script");script.type="text/javascript";script.src=src;head.appendChild(script);}
var setIfrHeight=function(){try{if(!window.xMsg){return;}
if(!xMsg.isInit){xMsg.init("qcloud");xMsg.setTarget("open",window.top);xMsg.isInit=true;}
if(window.parent!=window.top){if(parent.setFrameHeight){xMsg.call("open","setFrameHeight",{height:800},function(ret){});return;}}
if(window.qcloud){if(window.qcloud.loadBalanceManageV2||window.qcloud.loadBalanceViewV2||window.qcloud.ticket||window.qcloud.mobileSpeedUp){xMsg.call("open","setFrameHeight",{height:800},function(ret){});return;}}
var height=document.body.clientHeight;if(Math.abs(height-ifrHeight)>10){ifrHeight=height;xMsg.call("open","setFrameHeight",{height:ifrHeight},function(ret){});}}
catch(e){}}
var domain='';try{domain=window.top.document.domain}
catch(e){}
if(!/qcloud\.com$/i.test(domain)){insertStyle('html{background:none}.nav_area_1,.head_v2,mod_customer_service,.foot_v2{display:none;}.j_home{visibility:hidden}.mod_form{background:#fff}');insertJS("http://qzonestyle.gtimg.cn/open/operate/mlib/widget/x-msg.js");var ifrHeight=0,isInit;window.iframeInit=function(){setIfrHeight();if(isInit){return;}
var msgEl=document.getElementById("iframe_msg_span");var bubEl=document.getElementById("important_msg_tip");var btnCart=document.getElementById("nav_btn_shopcart");var btnBuy=document.getElementById("nav_btn_buy");var serviceDiv=document.getElementById("foot_customer_service");if(msgEl){msgEl.style.display="";if(bubEl){bubEl.style.left=(-70)+'px';bubEl.style.top=28+'px';msgEl.appendChild(bubEl);}
isInit=true}
if(btnCart){btnCart.removeAttribute("target");isInit=true}
if(btnBuy){btnBuy.removeAttribute("target");isInit=true}
if(serviceDiv){serviceDiv.parentNode.removeChild(serviceDiv);isInit=true}}
setInterval(iframeInit,100);if(/passport\.qcloud\.com/i.test(window.location.host)){setTimeout(function(){if(window.xMsg){xMsg.call("open","sessionEnd");}
else{setTimeout(arguments.callee,60);}},30);}}}})();/*  |xGv00|437cd92df9bf416d1aef243e1cbbf5f4 */