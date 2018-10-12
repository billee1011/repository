
function createNumDiv(index,value){
	var ele = $('#'+index);
	if(ele.length<=0){
		var div = '<div class="digit" id="'+index+'" >'+
			   '<div class="fade">&nbsp;</div>'+
				'<span class="num top" id="'+index+'-top">'+value+'</span>'+
				'<span class="num bottom" id="'+index+'-bottom">'+
				  '<div class="bottom-container">'+value+'</div></span>'+
				  '<div class="swapper">'+
					'<div id="top-'+index+'-anim" class="num-anim top-anim" style="display:none;">'+
					  '<div class="top-half-num">'+value+'</div>'+
					'</div>'+
					'<div id="bottom-'+index+'-anim" class="num-anim bottom-anim" style="display:none;">'+
					  '<div class="bottom-half-num">'+
						'<div class="dropper">'+value+'</div></div>'+
					'</div>'+
				'</div>'+
				'<div class="ring ring-left"></div>'+
				'<div class="ring ring-right"></div>'+
			  '</div>';
			  var containIndex = parseInt(index/3);
	    if($('#container_'+containIndex).length<=0){
			var mainDiv = '<div class="time-container seconds" id="container_'+containIndex+'" >' + div + '</div>';
			$(mainDiv).insertBefore($('#clock').children()[0]);
		}
		else{
			$(div).insertBefore($('#container_'+containIndex).children()[0]);
		}	
	}
	else{
	/*
	    $('#' + index + '-top').text(value);
		$('.bottom-container',$('#' + index + '-bottom')).text(value);
		var top =  $('#top-' + index + '-anim');
		var bottom =  $('#bottom-' + index + '-anim');
		 $('.top-half-num', top).text(value);
	     $('.dropper', bottom).text(value);**/
	}
	
}

function updateTime(strData) {
	for (var i=0;i<strData.length;i++) {
		  var value = strData[strData.length-i-1];
		  createNumDiv(i,value);
	      setTimes(i,value);
	}
}

function timeToString(currentTime) {
  var t;
  t = currentTime.toString();
  if (t.length === 1) {
    return '0' + t;
  }
  return t;
}

function getPreviousTime(type) {
  return $('#' + type + '-top').text();
}

function setTimes(type, timeStr) {
  setTime( $('#' + type + '-top').text(), timeStr, type);
}

function setTime(previousTime, newTime, type) {
  if (previousTime === newTime) {
    return;
  }
  setTimeout(function() {
    $('#' + type + '-top').text(newTime);
  }, 150);
  setTimeout(function() {
  $('.bottom-container',
    $('#' + type + '-bottom')).text(newTime);
  }, 365);
  animateTime(previousTime, newTime, type);
}

function animateTime(previousTime, newTime, type) {
  var top, bottom;
  top = $('#top-' + type + '-anim');
  bottom = $('#bottom-' + type + '-anim');
  $('.top-half-num', top).text(previousTime);
  $('.dropper', bottom).text(newTime);
  top.show();
  bottom.show();
  $('#top-' + type + '-anim').css('visibility', 'visible');
  $('#bottom-' + type + '-anim').css('visibility', 'visible');
  animateNumSwap(type);
  setTimeout(function() {
    hideNumSwap(type);
  }, 365);
}

function animateNumSwap(type) {
  $('#top-' + type + '-anim').toggleClass('up');
  $('#bottom-' + type + '-anim').toggleClass('down');
}

function hideNumSwap(type) {
  $('#top-' + type + '-anim').toggleClass('up');
  $('#bottom-' + type + '-anim').toggleClass('down');
  $('#top-' + type + '-anim').css('visibility', 'hidden');
  $('#bottom-' + type + '-anim').css('visibility', 'hidden');
}

window.requestAnimFrame = (function(callback){ 
    return window.requestAnimationFrame || 
        window.webkitRequestAnimationFrame || 
        window.mozRequestAnimationFrame || 
        window.oRequestAnimationFrame || 
        window.msRequestAnimationFrame || 
        function(callback){ window.setTimeout(callback, 1000 / 60); }
})();
