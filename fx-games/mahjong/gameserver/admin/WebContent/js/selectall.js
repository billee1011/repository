function toShow(draggableSelector, maskHide){
	  if(!maskHide){
		  $('#mask_div').show();
	  }
	  $('#formDiv').css("position", "absolute");
	  var left = ($('.content_main').width() - $('#formDiv').width()) / 2;
	  $('#formDiv').css("left", left);
	  $('#formDiv').show();
	  $('#formDiv').draggable({handle:draggableSelector});
 }
 function cancel(){
	  $('#mask_div').hide();
	  $('#formDiv').hide();
 }
 
 function popSubmit(successCallback){
	 var action = $("#postform").attr("action");
	 
	 $.ajax({
         cache: true,
         type: "POST",
         url:action,
         data:$('#postform').serialize(),// 你的formid
         async: false,
         error: function(request, errorMsg) {
             $('#pop_error').html(errorMsg);
         },
         success: function(data) {
        	 if(successCallback){
        		 successCallback(data);
        	 }else{
        		 $('.content_main').html(data);
        	 }
         }
     });
 }

function parentBindChangeEvent(key){
	$('[checkname^=' + key + ']').change(function(){
		var clzz = $(this).attr('checkname');
		var checked = $(this).is(":checked");
		if(checked){
			$('[checkname^=' + clzz + ']').prop("checked", true);
		}else{
			$('[checkname^=' + clzz + ']').removeAttr("checked");
		}
	});
 }
 function checkRoot(key){
	 var root = $('[checkroot=' + key + ']');
	 if(root.size() > 0){
		 root.prop("checked", true);
		
		 root.each(function(){
			 var parent = $(this);
			 var clzz = parent.attr('checkname');
			 $('[checkname^=' + clzz + ']').each(function(){
				 if(!$(this).is(":checked")){
					 parent.removeAttr("checked");
					 return false;
				 }
			 });
		 });
	 }
 }
 
 function validPassword(value1, value2, errorId, nullMsg, notSameMsg){
	 if(value1.length == 0 || value2.length == 0){
		 $('#' + errorId).html(nullMsg);
	 }else if(value1 != value2){
		 $('#' + errorId).html(notSameMsg);
	 }
 }
 
 function onlyInputInt(){
     //文本框限输入整数            
     $("input.intonly").keydown(function () {
         var e = $(this).event || window.event;
         var code = parseInt(e.keyCode);
         if (code >= 96 && code <= 105 || code >= 48 && code <= 57 || code == 8) {
             return true;
         } else {
             return false;
         }
     });
 }
 
 function onlyInputNumber(){
	//文本框限输入整数或浮点数           
     $("input.numberOnly").keydown(function () {
         var e = $(this).event || window.event;
         var code = parseInt(e.keyCode);
         if(code == 110 || code == 190){
        	 var nowValue = $(this).val();
        	 if(nowValue.length > 0 && nowValue.indexOf('.') < 0){
        		 return true;
        	 }
        	 return false;
         }
         if (code >= 96 && code <= 105 || code >= 48 && code <= 57 || code == 8) {
             return true;
         } else {
             return false;
         }
     });
 }
 
 //判断是否是数字
 function isNumber(str){
	for(var i = 0; i < str.length; i++){
		var ch = str.charAt(i);
		if(ch < '0' || ch > '9'){
			return false;
		}
	}
	return true;
 }
 
 function bfValidate(obj, errorSelector, name){
		if($(obj).hasClass('notnull') && $(obj).val().length == 0){
			$(errorSelector).html(name + '不能为空');
			return false;
		}
		
		if($(obj).hasClass('intonly')){
			var value = $(obj).val();
			if(!isNumber(value)){
				$(errorSelector).html(name + '必须为整数');
				return false;
			}
		}
		
		if($(obj).hasClass('numberonly')){
			var value = $(obj).val();
			var numberReg = /^[0-9]+((\.?[0-9]+)|[0-9]*)$/;
			if(!numberReg.test(value)){
				$(errorSelector).html(name + '必须为数值类型');
				return false;
			}
		}
		return true;
	}
