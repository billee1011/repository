// 平台修改  区服修改
function selectChange(obj, targetId, url, id, context){
	var action = context + '/' + url + '.do';
	var $target = $('#' + targetId);
	$.post(action, {'id':id}, function(data){
	window.location = context + "/center.do";
	});
}

function formatDate(date){
	var year = date.getFullYear();
	var month = checkTime(date.getMonth() + 1);
	var day = checkTime(date.getDate());
	
	var hour = checkTime(date.getHours());
	var min = checkTime(date.getMinutes());
	var sec = checkTime(date.getSeconds());
	
	return year + "-" + month + "-" + day + " " + hour + ":" + min + ":" + sec;
}

function toSelectArea(curPage, context){
	var numPerPage = 100;
	$.get(context + "/changearea.do", {'curpage':curPage, 'countpp':numPerPage}, function(data, status){
		$('#center_mask_div').show();
		  $('#selectarea').css("position", "absolute");
		  $('#selectarea').html(data);
		  var left = ($('body').width() - $('#selectarea').width()) / 2;
		  $('#selectarea').css("left", left);
		  $('#selectarea').show();
	});
}

function toSelectCrossArea(curPage, context, crossType, callback){
	var numPerPage = 100;
	$.get(context + "/crossarea.do", {'curpage':curPage, 'countpp':numPerPage, 'crosstype':crossType, 'callback':callback}, function(data, status){
		$('#crossformDiv').html(data);
		popUp2("crossformDiv", ".pop_layer_title");
	});
}

function selectCrossArea(area, errorId, callback){
	if(area == -1){
		$('#' + errorId).html("没有选择一个区服");
	}else{
		eval(callback + "(" + area + ")");
		cancelPop("crossformDiv");
	}
}

function selectArea(area, errorId, context){
	if(area == -1){
		$('#' + errorId).html("没有选择一个区服");
	}else{
		$.post(context + "/changearea.do", {'id':area}, function(data, status){
			if(data.success == 0){
				var $displayCurArea = $('#displayCurrentArea span');
				$($displayCurArea[0]).html(data.id);
				$($displayCurArea[1]).html(data.name);
				hideSelectArea();
				window.location = context + "/center.do";
			}else{
				$('#' + errorId).html('error, errorcode: ' + data.success);
			}
		});
	}
}

function hideSelectArea(){
	$('#center_mask_div').hide();
	$('#selectarea').hide();
	$('#selectarea').html('');
}

function chooseArea(areaObj, areaId, targetSaverId){
	$('.areaSelected').removeClass('areaSelected');
	$(areaObj).addClass('areaSelected');
	$('#' + targetSaverId).attr('value', areaId);
}

function checkTime(i)
{
if (i<10) 
  {i="0" + i;}
  return i;
}

function toSelMultiArea(curPage, context,isAllArea){
	var numPerPage = 100;
	var normalOrder = $('.areanormalorder').val();
	var allavailablepf = $('.allavailablepf').val();
	$.get(context + "/multiarea.do", {'curpage':curPage, 'countpp':numPerPage, 'normalOrder':normalOrder, 'allavailablepf':allavailablepf}, function(data, status){
		  var $dialog = $('#mulSelectAreaDiv');
		  $dialog.find('.muliDisplayArea:first').html(data);
		  $dialog.css("position", "absolute");
		  var left = ($('body').width() - $dialog.width()) / 2;
		  $dialog.css("left", left + "px");
		  $dialog.show();
		  $dialog.draggable();
	});
	if(isAllArea){
		$("#mulSelectAreaDiv input.allAreaOption").attr('checked',true);
	}
}

function multiAssure(){
	$('#mulSelectAreaDiv').hide();
	if(isAllAreas()){
		$('.multiResultAllArea').val('1');
		if(isExitsFunction('worldFun')){
			worldFun('-1');
		}
	}else{
		$('.multiResultAllArea').val('0');
		$('.multiResultAreas').val(getSelectedAreas());
		var worlds = getSelectedWorlds();
		$('.multiResultWorlds').val(worlds);
		if(isExitsFunction('worldFun')){
			worldFun(worlds);
		}
	}
}

//是否存在指定函数 
function isExitsFunction(funcName) {
    try {
        if (typeof(eval(funcName)) == "function") {
            return true;
        }
    } catch(e) {}
    return false;
}

function multiCancel(){
	$('#mulSelectAreaDiv').hide();
}

var areaCache = {};
var worldCache = {};

function multiChooseArea(areaObj, areaId, targetSaverId, dialogId, force, worldId){
	if(!force){ //非强制模式  从区不允许选择
		var followerId = $(areaObj).attr('follower');
		if(followerId != 0){
			return;
		}
	}
	
	
	var $dialog = $('#' + dialogId);
	var $targetSaver = $dialog.find('.' + targetSaverId + ':first');
	var value = $targetSaver.val();
	var isDefault = false;
	var $defaultSelectFlagObj;
	if($(areaObj).hasClass('defaultSelectArea')){
		isDefault = true;
		$defaultSelectFlagObj = $dialog.find('.defaultSelectFlag:first');
	}
	
	var followerId = $(areaObj).attr('follower');
	
	if($(areaObj).hasClass('areaSelected')){
		$(areaObj).removeClass('areaSelected');
		delete areaCache[areaId];
		if(worldId){
			delete worldCache[worldId];
		}
		if(isDefault){
			$defaultSelectFlagObj.val('0');
		}
		
		if(followerId == 0){
			$('[follower=' + $(areaObj).attr('areaId') + ']').each(function(){
				if($(this).hasClass('areaSelected')){
					var followWorldId;
					if(worldId){
						followWorldId = $(this).attr('worldId');
					}
					multiChooseArea(this, $(this).attr('areaId'), targetSaverId, dialogId, true);
				}
			});
		}
		
	}else{
		$(areaObj).addClass('areaSelected');
		areaCache[areaId] = areaId;
		if(worldId){
			worldCache[worldId] = worldId;
		}
		if(isDefault){
			$defaultSelectFlagObj.val('1');
		}
		
		if(followerId == 0){
			$('[follower=' + $(areaObj).attr('areaId') + ']').each(function(){
				if(!$(this).hasClass('areaSelected')){
					multiChooseArea(this, $(this).attr('areaId'), targetSaverId, dialogId, true);
				}
			});
		}
	}
	$targetSaver.val(value);
}

function allSelectOrNot(obj){
	var $dialog = $('#mulSelectAreaDiv');
	var $selectd = $(obj).filter(':checked');
	if($selectd.size() > 0){ //选中状态
		$dialog.find('a.multiSelectArea').each(function(){
			if(!$(this).hasClass('areaSelected')){
				$(this).click();
			}
		});
	}else{
		$dialog.find('a.multiSelectArea').each(function(){
			if($(this).hasClass('areaSelected')){
				$(this).click();
			}
		});
	}
}
//多选服务器参数重置
function multiAreaReset(){
	areaCache = {};
	worldCache = {};
	$('#mulSelectAreaDiv .virginSet').val('');	
}

function multiAreasFillParams(mapObj){
	if(isAllAreas()){
		mapObj['all'] = true;
	}else{
		mapObj['areaList'] = getSelectedAreas();
	}
}

function isMultiSelValid(){
	if(isAllAreas()){
		return true;
	}
	var count = 0;
	$.each(areaCache,function(key,value){  
		count++;
		return false;
	});
	return count > 0;
}

function isAllAreas(){
	return $("#mulSelectAreaDiv input.allAreaOption:checked").size() > 0;
}

function getSelectedAreas(){
	var ret = "";
	$.each(areaCache,function(key,value){  
		if(ret.length > 0){
			ret = ret + ",";
		}
		ret = ret + key;
	});
	return ret;
}

function getSelectedWorlds(postfix){
	var ret = "";
	if(!postfix){
		postfix = '';
	}
	$.each(worldCache,function(key,value){  
		if(ret.length > 0){
			ret = ret + ",";
		}
		ret = ret + key + postfix;
	});
	return ret;
}

function autoSelectSelectedAreas(targetSaverId){
	var $multiResultAreas = $('.multiResultAreas');
	if($multiResultAreas.size() > 0){
		var areas = $multiResultAreas.val();
		var start = 0;
		var splitIndex = -1;
		do{
		    splitIndex = areas.indexOf(',', start);
		    var subStr = ""; 
		    if(splitIndex == -1){
		    	subStr = areas.substring(start);
		    }else{
		    	subStr = areas.substring(start, splitIndex);
		    	start = splitIndex + 1;
		    }
		    
		    if(subStr != undefined && subStr.length > 0){
			    var id = parseInt(subStr);
			    areaCache[id] = id;
		    }
		    
		}while(splitIndex != -1);
	}
	var $multiResultWorlds = $('.multiResultWorlds');
	if($multiResultWorlds.size() > 0){
		var areas = $multiResultWorlds.val();
		var start = 0;
		var splitIndex = -1;
		do{
			splitIndex = areas.indexOf(',', start);
			var subStr = ""; 
			if(splitIndex == -1){
				subStr = areas.substring(start);
			}else{
				subStr = areas.substring(start, splitIndex);
				start = splitIndex + 1;
			}
			
			if(subStr != undefined && subStr.length > 0){
				var id = parseInt(subStr);
				worldCache[id] = id;
			}
			
		}while(splitIndex != -1);
	}
	$.each(areaCache,function(key,value){  
		$('a.withId_' + key).addClass('areaSelected');
	});
}

// 国家选择通用
function toSelMultiCountry(curPage, context){
	var numPerPage = 100;
	
	$.get(context + "/os/country/multicountry.do", {'curpage':curPage, 'countpp':numPerPage}, function(data, status){
		  var $dialog = $('#mulSelectCountryDiv');
		  $dialog.find('.muliDisplayArea:first').html(data);
		  $dialog.css("position", "absolute");
		  var left = ($('body').width() - $dialog.width()) / 2;
		  $dialog.css("left", left + "px");
		  $dialog.show();
		  $dialog.draggable();
	});
}

function multiCountryAssure(){
	$('#mulSelectCountryDiv').hide();
	if(isAllAreas()){
		$('.multiResultAllCountry').val('1');
	}else{
		$('.multiResultAllCountry').val('0');
		$('.multiResultCountries').val(getSelectedCountries());
	}
}

function multiCountryCancel(){
	$('#mulSelectCountryDiv').hide();
}

var countryCache = {};

function multiChooseCountry(areaObj, areaId, targetSaverId, dialogId){
	var $dialog = $('#' + dialogId);
	var $targetSaver = $dialog.find('.' + targetSaverId + ':first');
	var value = $targetSaver.val();
	var isDefault = false;
	var $defaultSelectFlagObj;
	if($(areaObj).hasClass('defaultSelectArea')){
		isDefault = true;
		$defaultSelectFlagObj = $dialog.find('.defaultSelectFlag:first');
	}
	if($(areaObj).hasClass('areaSelected')){
		$(areaObj).removeClass('areaSelected');
		delete countryCache[areaId];
		if(isDefault){
			$defaultSelectFlagObj.val('0');
		}
	}else{
		$(areaObj).addClass('areaSelected');
		countryCache[areaId] = areaId;
		if(isDefault){
			$defaultSelectFlagObj.val('1');
		}
	}
	$targetSaver.val(value);
}

function allCountrySelectOrNot(obj){
	var $dialog = $('#mulSelectCountryDiv');
	var $selectd = $(obj).filter(':checked');
	if($selectd.size() > 0){ //选中状态
		$dialog.find('a.multiSelectArea').each(function(){
			if(!$(this).hasClass('areaSelected')){
				$(this).click();
			}
		});
	}else{
		$dialog.find('a.multiSelectArea').each(function(){
			if($(this).hasClass('areaSelected')){
				$(this).click();
			}
		});
	}
}
//多选国家参数重置
function multiCountryReset(){
	countryCache = {};
	$('#mulSelectCountryDiv .virginSet').val('');	
}

function multiCountriesFillParams(mapObj){
	if(isAllAreas()){
		mapObj['all'] = true;
	}else{
		mapObj['countryList'] = getSelectedCountries();
	}
}

function isMultiCountrySelValid(){
	if(isAllCountries()){
		return true;
	}
	var count = 0;
	$.each(countryCache,function(key,value){  
		count++;
		return false;
	});
	return count > 0;
}

function isAllCountries(){
	return $("#mulSelectCountryDiv input.allAreaOption:checked").size() > 0;
}

function getSelectedCountries(){
	var ret = "";
	$.each(countryCache,function(key,value){
		if(ret.length > 0){
			ret = ret + ",";
		}
		ret = ret + key;
	});
	return ret;
}

function autoSelectSelectedCountries(){
	var $multiResultCountries = $('.multiResultCountries');
	if($multiResultCountries.size() > 0){
		var countries = $multiResultCountries.val();
		var start = 0;
		var splitIndex = -1;
		do{
		    splitIndex = countries.indexOf(',', start);
		    var subStr = ""; 
		    if(splitIndex == -1){
		    	subStr = countries.substring(start);
		    }else{
		    	subStr = countries.substring(start, splitIndex);
		    	start = splitIndex + 1;
		    }
		    
		    if(subStr != undefined && subStr.length > 0){
		    	var id = parseInt(subStr);
		    	countryCache[id] = id;
		    }
		    
		}while(splitIndex != -1);
	}
	$.each(countryCache,function(key,value){  
		$('a.withCountryId_' + key).addClass('areaSelected');
	});
	
}

//-----------------补偿js B----------------
//批量上传角色成功
function batchPlayersUploadSuccess(data, status){
	var errCode =  data.errorCode;
	if(errCode.length > 0){
		$('#batch_players_pop_error').html(errCode);
	}else{
		$('#batch_players').hide();
	}
	var $batchPlayersTbody = $('#role_display_body_table tbody');
	for(var i = 0; i < data.playerVos.length; i++){
			var playerVo = data.playerVos[i];
			var pid = playerVo.id;
			var name = playerVo.name;
			realAddPlayerToTable(pid, name, $batchPlayersTbody);
	}
}

function realAddPlayerToTable(pid, name, $batchPlayersTbody){
	if($batchPlayersTbody == null){
		$batchPlayersTbody = $('#role_display_body_table tbody');
	}
	var array = [pid, name, commonDelete];
	var tempStr = batchPlayersTr;
	for(var j = 0; j < array.length; j++){
		var index = tempStr.indexOf('{}');
		tempStr = tempStr.substring(0, index) + array[j]
         + tempStr.substring(index + 2);
	}
	$batchPlayersTbody.prepend(tempStr);
}

function realAddPlayerNameToTable(name, $batchPlayersTbody){
	if($batchPlayersTbody == null){
		$batchPlayersTbody = $('#role_display_body_table tbody');
	}
	var array = [name, commonDelete];
	var tempStr = batchPlayersTr;
	for(var j = 0; j < array.length; j++){
		var index = tempStr.indexOf('{}');
		tempStr = tempStr.substring(0, index) + array[j]
         + tempStr.substring(index + 2);
	}
	$batchPlayersTbody.prepend(tempStr);
}

//upload事件
function batchPendingPlayersUpload(pData){
	if(!pData){
		pData = {};
	}
	if(!pData.selector){
		pData.selector = '#batch_players_upload';
	}
	if(!pData.url){
		pData.url = '/pss/batchPlayersUpload.do';
	}
	if(!pData.callback){
		pData.callback = batchPlayersUploadSuccess;
	}
	$(pData.selector).fileupload({
        url: $('#ctx').val() + pData.url,
        dataType: 'json',
        autoUpload: false,
        add: function(e, data){
        	$('#uploadsubmit').click(function(){
        		if($("#allAreas")){
        			if($("#allAreas").val()==""&&$("#worldIds").val()==""){
        				alert("请选择呢游戏区！");
        				return;
        			}
        		}
        		data.submit().done(function(data, status){
        			pData.callback(data, status);
        		}).progress(function(e, data) {
    	            var progress = parseInt(data.loaded / data.total * 100, 10);
    	            $('#progress .progress-bar').css(
    	                'width',
    	                progress + '%'
    	            );
    	        });
        	});
        }
   });
}

function addRoleNameToTable(){
	var roleName = $('#role_name').val();
	if(roleName.length == 0){
		$('#role_error').html("角色名不能为空");
	}else{
		realAddPlayerNameToTable(roleName);
	}
}

function addRoleToTable(){
	var roleId = $('#role_id').val();
	var roleName = $('#role_name').val();
	if(roleId.length == 0 || roleName.length == 0){
		$('#role_error').html("角色id和角色名不能为空");
	}else{
		realAddPlayerToTable(roleId, roleName);
	}
}

//把选择的item添加到table
function addItemToTable(){
	var itemcount = $('#selected_itemcount').val();
	if(itemcount.length == 0 || !isNumber(itemcount)){
		$('#itemcount_error').html("道具数量必须为整数");
	}else{
    	var itemid = $('#selected_itemid').val();
    	var itemname = $('#selected_itemname').val();
		if(itemid.length == 0 || itemname.length == 0){
			$('#itemcount_error').html("必须选择一个道具");
		}else{
			var bindOrNot = $('#selected_bindornot').val();
			var extendItemShuxing=$("#itemExtend").val();
			if(extendItemShuxing!=""){
				//解析自定义装备的属性
				var extendItemArray=extendItemShuxing.split(":");
				itemid=extendItemArray[0];
				var itemCache = genItems;
		    	for(var i = 0; i < itemCache.length; i++){
		    		var itemTemplate = itemCache[i];
		    		if(itemid==itemTemplate.id){
		    			itemname=itemTemplate.name;
		    			break;
		    		}
		    	}
		    	itemcount=extendItemArray[1];
		    	bindOrNot=extendItemArray[2];
			}
			$("#itemExtend").val("");
			extendItemShuxing="<a href=\"#\" title=\""+extendItemShuxing+"\">属性</a>";
			realAddItemToTable(itemid, itemname, itemcount, bindOrNot,extendItemShuxing);
		}
	}
}

function realAddItemToTable(itemid, itemname, itemcount, bindOrNot,extendItemShuxing){
	var array = [itemid, itemname, itemcount, bindOrNot,extendItemShuxing,commonDelete];
	var tempStr = itemTr;
	for(var i = 0; i < array.length; i++){
		var index = tempStr.indexOf('{}');
		tempStr = tempStr.substring(0, index) + array[i]
         + tempStr.substring(index + 2);
	}
	
	$('#item_display_body_table tbody').prepend(tempStr);
}

function removeFromTable(obj){
	$(obj).closest('tr').remove();
}

//道具id输入框KeyUp事件
function selectItemIdKeyup(){
	var itemid = $('#selected_itemid').val();
	if(itemid.length == 0){
		$('#selected_itemname').val('');
	}else{
		var $selOption = $('#item_choose_list option[value=' + itemid + ']');
    	if($selOption.size() == 0){
    		$('#selected_itemname').val('');
    	}else{
    		$('#selected_itemname').val($selOption.html());
    	}
	}
}

function chooseItem(){
	var $selOption = $('#item_choose_list option:selected');
	if($selOption.size() == 0&&$("#itemExtend").val()==""){
		$('#item_templates_error').html("请选择一个道具/装备");
	}else{
		if($selOption.size() == 0){
			var tempExtendItem=$("#itemExtend").val();
			var tempExtendItemId=tempExtendItem.split(":")[0];
			$('#selected_itemid').val(tempExtendItemId);
			var itemCache = genItems;
	    	for(var i = 0; i < itemCache.length; i++){
	    		var itemTemplate = itemCache[i];
	    		if(tempExtendItemId==itemTemplate.id){
	    			$('#selected_itemname').val(itemTemplate.name);
	    			break;
	    		}
	    	}
			
			$('#item_templates').hide();
		}else{
			$('#selected_itemid').val($selOption.val());
			$('#selected_itemname').val($selOption.html());
			$('#item_templates').hide();
		}
		
	}
}

//计算upload控件坐标和大小
function calBatchUploadPlayerLocation(){
	var $upfile = $('#batch_players_upload');
	var $upscan = $('#batch_scan');
	$upfile.css('margin-left', '-' + (parseInt($upscan.width()) * 2 + 10) + 'px');  //试出来的
	$upfile.css('width', ($upscan.width() * 2 - 10) + 'px');
	$upfile.css('height', $upscan.height());
}

//重置  重新请求一次吧
function redeemReset(){
	$.get($('#ctx').val() + '/pss/redeem/index.do', function(data){
		$(".content_main").html(data);
	});
}

//邮件模板选择变化
function mailSelectChange(){
	var $option = $('#mail_select_title option:selected');
	$('#mailtitle').val($option.html());
	var mailContent = "";
	for(var i = 0; i < mailCache.length; i++){
		var mailTemplate = mailCache[i];
		if(mailTemplate.id == $option.val()){
			mailContent = mailTemplate.content;
			break;
		}
	}
	$('#mailcontent').val(mailContent);
}

function addMailTemplateToSelect(data, addToCache){
	if(addToCache){
		mailCache.push(data);
	}
	$('#mail_select_title').append('<option value="' + data.id + '">' + data.title + '</option>');
}

function updateMailTemplateToSelect(data){
	var titleUpdate = false;
	for(var i = 0; i < mailCache.length; i++){
		var mailTemplate = mailCache[i];
		if(mailTemplate.id == data.id){
			if(mailTemplate.title != data.title){
			    mailTemplate.title = data.title;
			    titleUpdate = true;
			}
			mailTemplate.content = data.content;
			break;
		}
	}
	if(titleUpdate){
		$('#mail_select_title option[value=' + data.id + ']').html(data.title);
	}
}

function deleteMailTemplateToSelect(data){
	for(var i = 0; i < mailCache.length; i++){
		var mailTemplate = mailCache[i];
		if(mailTemplate.id == data.id){
			mailCache.splice(i, 1);
			break;
		}
	}
	$('#mail_select_title option[value=' + data.id + ']').remove();
	mailSelectChange();
}

function popUp(id, draggableSelector){
	var $obj = $('#' + id);
	if($obj.is(":hidden")){
		$obj.css('position', 'absolute');
		$obj.css('left', $('#content_area').width() - $($('#content_area .to_redeem_div')[0]).width());
		$obj.css('top', $('#content_area').height() / 2);
		$obj.show();
		if(draggableSelector == undefined){
			$obj.draggable();
		}else{
			$obj.draggable({handle:draggableSelector});
		}
	}
}

function popUp2(id, draggableSelector){
	var $obj = $('#' + id);
	$obj.css('position', 'absolute');
	$obj.css('left', ($('.content_main').width() - $obj.width()) / 2);
	$obj.css('top', ($('.content_main').height() - $obj.height()) / 4);
	$obj.show();
	if(draggableSelector == undefined){
		$obj.draggable();
	}else{
		$obj.draggable({handle:draggableSelector});
	}
}

function cancelPop(id){
	$('#' + id).hide();
}

function deleteMailTemplate(){
	$('#mail_pop_error').html('');
	var $option = $('#mail_select_title option:selected');
	var id = $option.attr('value');
	if(id == -1){
		$('#mail_pop_error').html('请选择一个模板');
	}else{
		$.post($('#ctx').val() + "/pss/redeem/deletemailtemplate.do", {'id':id}, function(data){
			if(data.id == -1){
				$('#mail_pop_error').html('删除失败， 错误码：' + data.title);
			}else{
				deleteMailTemplateToSelect(data);
			}
		});
	}
}

function modifyMailTemplate(){
	$('#mail_pop_error').html('');
	var $option = $('#mail_select_title option:selected');
	var id = $option.attr('value');
	if(id == -1){
		$('#mail_pop_error').html('请选择一个模板');
	}else{
		var title = $('#mailtitle').val();
		var content = $('#mailcontent').val();
		if(title.length <= 0 || content.length <= 0){
			$('#mail_pop_error').html('邮件标题和内容不能为空');
		}else{
			$.post($('#ctx').val() + "/pss/redeem/modifymailtemplate.do", {'id':id, 'title':title, 'content':content}, function(data){
				if(data.id == -1){
					$('#mail_pop_error').html('修改失败， 错误码：' + data.title);
				}else{
					updateMailTemplateToSelect(data);
				}
			});
		}
	}
}

function addMailTemplate(){
	$('#mail_pop_error').html('');
	var title = $('#mailtitle').val();
	var content = $('#mailcontent').val();
	if(title.length <= 0 || content.length <= 0){
		$('#mail_pop_error').html('邮件标题和内容不能为空');
	}else{
		$.post($('#ctx').val() + "/pss/redeem/createmailtemplate.do", {'title':title, 'content':content}, function(data){
			if(data.id == -1){
				$('#mail_pop_error').html('增加失败， 错误码：' + data.title);
			}else{
				addMailTemplateToSelect(data, true);
			}
		});
	}
}

function calScrollTableWidth(headTableId, bodyTableId){
	$headTable = $('#' + headTableId);
	$bodyTable = $('#' + bodyTableId);
	$bodyTable.parent().css('padding', '0px');
	$bodyTable.parent().css('width', $headTable.css('width'));
	$bodyTable.css('margin-top', '-' + $bodyTable.find('tr').css('height'));
	var $headTableThs = $headTable.find('th');
	var $bodyTableThs = $bodyTable.find('th');
	var size = $bodyTableThs.size();
	$headTableThs.each(function(index, element){
		$($bodyTableThs[index]).css('width', $(element).css('width'));
	});
}

function redeemCommit(){
	var mailTitle = $("#mailtitle").val();
	if(mailTitle.length == 0){
		$('#mail_pop_error').html("邮件标题不能为空");
		return;
	}
	var mailContent = $("#mailcontent").val();
	if(mailContent.length == 0){
		$('#mail_pop_error').html("邮件内容不能为空");
		return;
	}
	
	var selectRoleType = $("select[name=role_type] option:selected").val();
	var roleArray = new Array();
	if(selectRoleType == 0){
		var roleNames="";
		//计算玩家信息
	    $("#role_display_body_table tbody tr[class=filledTr]").each(function(){
	    	var role = new Array();
	    	var $tds = $(this).children("td");
	    	var id = $($tds[0]).html();
	    	var name = $($tds[1]).html();
	    	role.push(id);
	    	role.push(name);
	    	roleArray.push(role.join(','));
	    	roleNames=roleNames+" "+name;
	    });
	  //道具信息
	    var items="";
	    $("#item_display_body_table tbody tr").each(function(){
	    	var $tds = $(this).children("td");
	    	var itemname = $($tds[1]).html();
	    	var itemcount = $($tds[2]).html();
	    	if(itemname!=""){
	    		items=items+" "+itemname+"("+itemcount+"个)";
	    	}
	    	
	    });
	    
		if(roleArray.length == 0){
			$('#mail_pop_error').html("没有角色信息");
			return;
		}
		//增加部分玩家补偿确认信息
		
		if(!window.confirm("你确认对"+roleNames+"玩家做如下补偿吗?物品明细:"+items)){
    		return;
    	}
	}else{
		//计算玩家信息
	    if($("#role_display_body_table tbody tr[class=filledTr]").size() > 0){
	    	if(!window.confirm("补偿确认，亲，你选择的是全部玩家补偿，但是还有单个角色信息，是否确认补偿？")){
	    		return;
	    	}else{
	    		if(!window.confirm("补偿二次确认，亲，你选择的是全部玩家补偿，但是还有单个角色信息，你真的确认要补偿么？")){
		    		return;
		    	}
	    	}
	    }else{
	    	if(!window.confirm("补偿确认，亲，你选择的是全服补偿，是否确认补偿？")){
	    		return;
	    	}
	    }
	}
	var money = $("#redeem_money").val();	
	var diamond = $("#redeem_diamond").val();
	//玩家补偿or邮件补偿
	var redeemType = $("#redeem_type").val();
	var itemArray = new Array();
	//计算玩家信息
    $("#item_display_body_table tbody tr[class=filledTr]").each(function(){
    	var item = new Array();
    	var $tds = $(this).children("td");
    	var id = $($tds[0]).html();
    	var name = $($tds[1]).html();
    	var count = $($tds[2]).html();
    	var bind = $($tds[3]).html();
    	var extendShuxing = $($tds[4]).html();
    	item.push(id);
    	item.push(name);
    	item.push(count);
    	item.push(bind);
    	item.push(extendShuxing);
    	itemArray.push(item.join(','));
    });
	
	if(!isMultiSelValid()){
		$('#mail_pop_error').html("没有选定一个区");
		return;
	}
	
	//填充选服信息
	var params = {'mailTitle':mailTitle,'mailContent':mailContent,'selectRoleType':selectRoleType,'roleArray':roleArray.join(','),'money':money,'diamond':diamond,'itemArray':itemArray.join(','),'redeemType':redeemType};
	multiAreasFillParams(params);
	var url=$('#ctx').val() + "/pss/redeem/redeem.do";
//	{\'mailTitle\':\'' + mailTitle + '\',\'mailContent\':\'' + mailContent + '\',\'selectRoleType\':\'' + selectRoleType + '\',\'roleArray\':\'' + roleArray + '\',\'money\':\'' + money + '\',\'diamond\':\'' + diamond + '\',\'itemArray\':\'' + itemArray + '\'}
	if(redeemType=="mail"){
		url=$('#ctx').val() + "/pss/redeem/redeemMail.do";
	}
	$.post(url, params, function(data){
		var msg = "失败";
		var startCheckResultFlag = false;
		if(!data.hasCheckPrivilege){
			if(data.retCode == 1&&redeemType!="mail"){
				msg = "成功，等待审核";
			}else if(data.retCode == 1&&redeemType=="mail"){
				msg = "成功";
			}else if(data.retCode == 1001&&redeemType!="mail"){  //异步操作  以日志记录为准
				msg = "成功，等待审核";
			}else if(data.retCode == 1001&&redeemType=="mail"){  //异步操作  以日志记录为准
				msg = "成功";
			}
		}else{
			if(data.retCode == 1){
				msg = "成功";
			}else if(data.retCode == 1001){  //异步操作  以日志记录为准
				msg = "成功: 以日志记录为准";
				startCheckResultFlag = true;
			}
		}
		if(!startCheckResultFlag){
			$('#redeemResultMsg').html(msg);
			$(".redeemResultError").html("");
			if(data.messages != null && data.messages.length > 0){
				for(var i = 0; i < data.messages.length; i++){
					$(".redeemResultError").append(data.messages[i] + '<br/>');
				}
			}
		}else{
			startCheckResult(data.serialId, ".redeemResultError", 'redeemResult');
		}
		popUp('redeemResult');
	});
}

function startCheckResult(serialId, selector, popSelect){
	var flag = false;
	var oldId = $('#multiResultPlace').val();
	if(oldId){
		var oldIntId = parseInt(oldId);
		clearInterval(oldIntId);
	}
	var id = setInterval(function(){
		$.get($('#ctx').val() + '/optresult.do', {"serialId":serialId}, function(data){
			if(data){
				$(selector).html(data);
				flag = true;
				// popUp(popSelect);
			}else if(flag){
				clearInterval(id);
			}
		});
	}, 500);
	$('#multiResultPlace').val(id);
}

//-----------------补偿js E----------------

//-----------------惩罚js B----------------
//重置  重新请求一次吧
function punishReset(){
	$.get($('#ctx').val() + '/pss/punish/index.do', function(data){
		$(".content_main").html(data);
	});
}

function punishCommit(){
	var roleName = $('#role_name').val();
	if(!roleName){
		$('#punish_pop_error').html("没有角色信息");
		return;
	}
	
	var beginTime = $('#punishBegin').val();
	var endTime = $('#punishEnd').val();
	
	var whyPunish = $('#whyPunish').val();
	if(whyPunish.length == 0){
		$('#punish_pop_error').html("请输入惩罚原因");
		return;
	}
	
	var selectPunishType = $("select[name=punish_type] option:selected").val();
	var params = {'selectPunishType':selectPunishType,'roleName':roleName, 'whyPunish':whyPunish, 'beginTime':beginTime, 'endTime':endTime,areaId:$('#search_type_btn').attr('value')};
	
	$.post($('#ctx').val() + "/pss/punish/punish.do", params, function(data){
		//<span id="redeemResultMsg" class="tip"></span>
		//   <div class="redeemResultError">
		var msg = "失败";
		if(data.retCode == 1){
			msg = "成功";
		}
		$('#punishResultMsg').html(msg);
		$(".punishResultError").html("");
		if(data.messages != null && data.messages.length > 0){
			for(var i = 0; i < data.messages.length; i++){
				$(".punishResultError").append(data.messages[i] + '<br/>');
			}
		}
		popUp('punishResult');
	});
}

function requestPunishList(areaId){
	$('#punishList').load($('#ctx').val() + "/pss/punish/list.do?areaId="+areaId, function(){
		toShowPunishList();
	});
}

function toShowPunishList(){
	  $('#punishList').css("position", "absolute");
	  var left = ($('.body').width() - $('#punishList').width()) / 2;
	  $('#punishList').css("left", left);
	  $('#punishList').show();
	  $('#punishList').draggable({handle:'.punishListTitle'});
 }
 function cancelPunishList(){
	  $('#punishList').hide();
 }
 
 function pagerFilter(data, thisObj){
	if (typeof data.length == 'number' && typeof data.splice == 'function'){	// is array
		data = {
			total: data.length,
			rows: data
		};
	}
	var dg;
	if(thisObj == undefined){
		dg = $(this);
	}else{
    	dg = $(thisObj);
	}
	var opts = dg.datagrid('options');
	var pager = dg.datagrid('getPager');
	pager.pagination({
		onSelectPage:function(pageNum, pageSize){
			opts.pageNumber = pageNum;
			opts.pageSize = pageSize;
			pager.pagination('refresh',{
				pageNumber:pageNum,
				pageSize:pageSize
			});
			dg.datagrid('loadData',data);
		}
	});
	if (!data.originalRows){
		data.originalRows = (data.rows);
	}
	var start = (opts.pageNumber-1)*parseInt(opts.pageSize);
	var end = start + parseInt(opts.pageSize);
	data.rows = (data.originalRows.slice(start, end));
	return data;
  }
//-----------------惩罚js E----------------
 
//=-------公告管理 B-------=
 function announceReset(){
		$('#announceInterval').val("0");
		$('#announceContent').val("");
		$('#createAnnounceStart').val("");
		$('#createAnnounceEnd').val("");
 }
	
	function announcingList(){
		$('#announcingList').load($('#ctx').val() + '/pss/announce/announcingList.do', function(data){
			toShowAnnoucingList();
		});
	}
	
	function announceHistory(){
		$('#announcingList').load($('#ctx').val() + '/pss/announce/announceHistory.do', function(data){
			toShowAnnoucingList();
		});
	}
	
	function toShowAnnoucingList(){
		  $('#announcingList').css("position", "absolute");
		  var left = ($('.body').width() - $('#announcingList').width()) / 2;
		  $('#announcingList').css("left", left);
		  $('#announcingList').show();
		  $('#announcingList').draggable({handle:'.annoucingListTitle'});
	 }
	
	function toShowAnnoucingDeleteDialog(){
		  $('#annoucingDeleteDialog').css("position", "absolute");
		  var left = ($('.body').width() - $('#annoucingDeleteDialog').width()) / 2;
		  $('#annoucingDeleteDialog').css("top", 50);
		  $('#annoucingDeleteDialog').css("left", left);
		  $('#annoucingDeleteDialog').show();
		  $('#annoucingDeleteDialog').draggable({handle:'.pop_layer_title'});
	 }
 
 function announceCommit(){
 	$('#announce_pop_error').val("");
 	var interval = $('#announceInterval').val();
 	if(interval.length == 0){
 		$('#announce_pop_error').html("时间间隔不能为空");
 		return;
 	}
 	
 	if(!isNumber(interval)){
 		$('#announce_pop_error').html("时间间隔必须为数字");
 		return;
 	}
 	
 	var content = editor.getContent();
 	if(content.length == 0){
 		$('#announce_pop_error').html("公告内容不能为空");
 		return;
 	}
 	
 	var start = $('#createAnnounceStart').val();
 	var end = $('#createAnnounceEnd').val();
 	
 	if(!isMultiSelValid()){
			$('#announce_pop_error').html("没有选定一个区");
			return;
		}
 	
 	var params = {content: content, interval:interval, startTime:start, endTime:end, };
		multiAreasFillParams(params);  //该方法会填充选区信息
 	
 	$.post($('#ctx').val() + '/pss/announce/create.do', params, function(data){
 		if(data == 1){
 			$('#announce_pop_error').html("添加公告成功");
 		}else{
 			$('#announce_pop_error').html("添加公告失败");
 		}
 	});
 }
 
 function serverReload() {
	 var globalChecked = $('#globalSelect:checked').size() > 0;
	if(!isMultiSelValid() && !isMultiSelCrossValid() && !globalChecked){
		$('#returnInfo_pop_error').html("没有选定一个区");
		return;
	}
	var params = {"globalSelect": globalChecked};
	multiAreasFillParams(params);
	multiCrossAreasFillParams(params);
	
	$.post($('#ctx').val() + '/ps/reload.do', params, function(data) {
		$(".content_main").html(data);
	});
	}
 
  function killOffPlayers(reason) {
		if (!isMultiSelValid()) {
			alert("没有选定一个区");
			return;
		}
		if(!reason){
			alert("请输入踢人原因");
			return;
		}
		
		var params = {'reason':reason};
		multiAreasFillParams(params);
		$.post($('#ctx').val() + '/ps/kickoff.do', params, function(data) {
			progressBar(function(){
				$(".content_main").html(data);
			});
		});
	}
 
	function serverMaintain(status) {
		if (!isMultiSelValid()) {
			$('#returnInfo_pop_error').html("没有选定一个区");
			return;
		}
		
		var reason = $("#reason").val();
		var params = {
			reason : reason,
			status : status
		};
		
		if(status == 1){//维护
			/*var foreseeOpenTime = $('#foreseeOpenTime').val();
			if(!foreseeOpenTime || foreseeOpenTime.length == 0){
				alert("请选择开服时间");
				return;
			}
			params['foreseeOpenTime'] = foreseeOpenTime;
			var maintainUrl = $('#maintainUrl').val();
			if(!maintainUrl || maintainUrl.length == 0){
				alert("请填写公告链接");
				return;
			}
			params['maintainUrl'] = maintainUrl;*/
		}
		
		
		multiAreasFillParams(params);
		
		var confirmStr = '';
		if(params.all){
			if(status == 1){
				confirmStr = '是否对所有服进行维护？';
			}else{
				confirmStr = '是否对所有服进行开启？';
			}
		}else{
			var worlds = getSelectedWorlds('服');
			if(status == 1){
				confirmStr = '是否对{' + worlds + '}进行维护？';
			}else{
				confirmStr = '是否对{' + worlds + '}所有服进行开启？';
			}
		}
		if(!window.confirm(confirmStr)){
			return;
		}
		
		$.post($('#ctx').val() + '/ps/maintain.do', params, function(data) {
			$(".content_main").html(data);
		});
	}
	
  function updateOpenTime(){
	  var foreseeOpenTime = $('#foreseeOpenTime').val();
	  if(!foreseeOpenTime || foreseeOpenTime.length == 0){
		  alert("请选择维护时间");
		  return;
	  }
	  $.post($('#ctx').val() + '/ps/updateOpenTime.do', {'foreseeOpenTime':foreseeOpenTime}, function(data) {
		 $(".content_main").html(data);
	  });
  }
  
  function updateMaintainUrl(){
	  var maintainUrl = $('#maintainUrl').val();
	  if(!maintainUrl || maintainUrl.length == 0){
		  alert("请填写维护公告地址");
		  return;
	  }
	  $.post($('#ctx').val() + '/ps/updatemaintainurl.do', {'maintainurl':maintainUrl}, function(data) {
		 $(".content_main").html(data);
	  });
  }
  
 function toDeleteAnnounce(url){
 	$.get(url, function(data){
 		$('#annoucingDeleteDialog').html(data);
 		$('#annoucingDeleteDialog').show();
 	});
 }
 
 function deleteAnnounce(url, params){
 	$.post(url, params, function(data){
 		if(data != -1){
 			var announcingData = $('#announcingListTable').datagrid('getData');
     	    var index = -1;
     	    for(var i = 0; i < announcingData.rows.length; i++){
     	    	if(announcingData.rows[i]['id'] == data){
     	    		index = i;
     	    	}
     	    }
     	    
     	    if(index >= 0){
 		    	$('#announcingListTable').datagrid('deleteRow', index);
 		    }
 		}else{
 			$('#announcingList_error').html('删除失败');
 		}
 	});
 }
 
 function commonPagination(selector, filter, params){
	var lf = pagerFilter;
	if(filter != undefined){
		lf = filter;
	}
	var initParams = {
			pagination:true,
			pageSize:50,
			striped:true,
			rownumbers:true,
			autoRowHeight:false,
			render:'frozen',
			singleSelect:true
		};
	if(params){
		for(var key in params){
			initParams[key] = params[key];
		}
	}
	
 	$(selector).datagrid(initParams).datagrid({loadFilter:lf});
 	
/*    	 var p = $('#punishListTable').datagrid('getPager');
 	
 	    $(p).pagination({  
 	        pageSize: 10,//每页显示的记录条数，默认为10  
 	        pageList: [5,10,15],//可以设置每页记录条数的列表  
 	        beforePageText: '第',//页数文本框前显示的汉字  
 	        afterPageText: '页    共 {pages} 页',  
 	        displayMsg: '当前显示 {from} - {to} 条记录   共 {total} 条记录',  
 	    });
 	    */
 }
 
 function commonServerPagination(selector, filter, url, extraParams,paraObj){
	    paraObj = paraObj || {};
		var lf = pagerFilter;
		if(filter != undefined){
			lf = filter;
		}
	 	$(selector).datagrid({
				pagination:true,
				pageSize:paraObj.pageSize||50,
				pageList:paraObj.pageList||[10,20,30,40,50,100,500,1000],
				singleSelect:true,
				striped:true,
				rownumbers:20,
				autoRowHeight:false,
				render:'frozen',
				nowrap:false,
				url:url,
				onBeforeLoad: function (param) {
					if(extraParams){
						for(var key in extraParams){
							if(key == 'editorField'){
								continue;
							}
							if(typeof extraParams[key] == 'function'){
								param[key] = extraParams[key]();
							}else{
								param[key] = extraParams[key];
							}
						}
					}
				},
				onLoadSuccess: function(data){
					
//					for(var i = 0; i < data.rows.length; i++){
//						if(loadSuccessCallback){
//							loadSuccessCallback(data.rows[i]);
//							$(selector).datagrid('refreshRow', i);
//						}
//					}
					
				},
				 onDblClickCell: function(index,field,value){
					 var hiddenEditingIndex = $(selector).parent().find('.hiddenEditingIndex').val();
					 if((hiddenEditingIndex && hiddenEditingIndex != index) || (extraParams && extraParams['editorField'] && extraParams.editorField.indexOf(field) == -1)){
						 return;
					 }
					 var editingIndex = $(selector).datagrid('beginEdit');
					 
					 $(selector).datagrid('beginEdit', index);
						var ed = $(selector).datagrid('getEditor', {index:index,field:field});
						if(ed){
							$(ed.target).focus();
						}
					}
			});
	 	
	/*    	 var p = $('#punishListTable').datagrid('getPager');
	 	
	 	    $(p).pagination({  
	 	        pageSize: 10,//每页显示的记录条数，默认为10  
	 	        pageList: [5,10,15],//可以设置每页记录条数的列表  
	 	        beforePageText: '第',//页数文本框前显示的汉字  
	 	        afterPageText: '页    共 {pages} 页',  
	 	        displayMsg: '当前显示 {from} - {to} 条记录   共 {total} 条记录',  
	 	    });
	 	    */
	 }
 
//=-------公告管理 E-------=
 /*
  * 序列化ajax提交表单
  */
 function serializeSubmit(formSelector, errorSelector, contentSelector, callback){
     var action = $(formSelector).attr("action");
	 $.ajax({
        cache: true,
        type: "POST",
        url:action,
        data:$(formSelector).serialize(),// 你的formid
        async: false,
        error: function(request, errorMsg) {
            $(errorSelector).html(errorMsg);
        },
        success: function(data) {
        	if(callback){
        		callback(data);
        	}else{
	        	if($(data).find('#errorcodeflag').size() == 0){
	        		$(contentSelector).html(data);
	        	}else{
	        		$(errorSelector).html(data);
	        	}
        	}
        }
    });
 }
 
 function ajaxALink(aSelector){
	 $(aSelector).unbind("click").click(function(){
		if(!$(this).hasClass("exclude")){
			var targetSelector = $(this).attr('targetSelector');
			if(targetSelector == null){
				targetSelector = '.content_main';
			}
			var url = $(this).attr('href');
			$.get(url, function(data){
				$(targetSelector).html(data);
			});
		}
		return false;
	});
 }
 function ajaxALink_(aSelector){
	 $(aSelector).each(function(){
		 var targetSelector = $(this).attr('targetSelector')
		 var self = this;
		 if(targetSelector){
			 $(this).unbind("click").click(function(){
					if(!$(self).hasClass("exclude")){
						var targetSelector = $(self).attr('targetSelector');
						if(targetSelector == null){
							targetSelector = '.content_main';
						}
						var url = $(self).attr('href');
						$.get(url, function(data){
							$(targetSelector).html(data);
						});
						
					}
					return false;
				});
		 }
	 });
 }
 
 function linkHref(urlObj){
	 var targetSelector = $(urlObj).attr('targetSelector');
		if(targetSelector == null){
			targetSelector = '.content_main';
		}
		var url = $(urlObj).attr("link");
		$.get(url, function(data){
			$(targetSelector).html(data);
		});
 }
 
 function calColumnWidth(rate){
	var total = $("div.table_area").width();
	return calFixWidth(total, rate);
}
 
 function calFixWidth(total, rate){
	 return total * rate;
 }

 function Util_ServerStatus(status){
	 if(status == 0) return "正常";
	 else if(status == 1) return "维护";
	 else if(status == 2) return "停服";
 }
 
 function closeDialog(){
	 $("#dialog_999").hide();
 }
 
 //
 function dialogInfoContent(data){
	 var $dialog = $("#dialog_999");
	 if($dialog.length<=0){
		 $(".content_main").append("<div id='dialog_999' class='pop_layer_v2' style='border: 2px solid black;'><div id='dialog_content' class='pop_layer_cont'></div><div class='pop_layer_ft'><a class='btn btn_submit not_close btn_blue' href='javascript:closeDialog();'><span class=''>确定</span></a><a class='btn btn_white_2 btn_close' href='javascript:closeDialog();'><span>取消</span></a></div></div>");
		 $dialog = $("#dialog_999");
	 }
	 $("#dialog_content").html("<div style='text-align:center;margin-left:auto; margin-right:auto;'>"+data+"</div>");
	  $dialog.css("position", "absolute");
	  $("#dialog_content").css("min-width","80px");
	  $("#dialog_content").css("min-height","80px");
	  var left = ($('body').width() - $dialog.width()) / 2;
	  $dialog.css("left", left + "px");
	  $dialog.show();
	  $dialog.draggable();
 }
 
 function dialogInfo(data){
	 var $dialog = $("#dialog_999");
	 if($dialog.length<=0){
		 $(".content_main").append("<div id='dialog_999' class='pop_layer_v2' style='border: 2px solid black;'><div id='dialog_content' class='pop_layer_cont'></div><div class='pop_layer_ft'><a class='btn btn_submit not_close btn_blue' href='javascript:closeDialog(func);'><span class=''>确定</span></a><a class='btn btn_white_2 btn_close' href='javascript:closeDialog();'><span>取消</span></a></div></div>");
		 $dialog = $("#dialog_999");
	 }
	 $("#dialog_content").html($("#"+data).html());
	  $dialog.css("position", "absolute");
	  var left = ($('body').width() - $dialog.width()) / 2;
	  $dialog.css("left", left + "px");
	  $dialog.show();
	  $dialog.draggable();
 }
 
 function timeStrAdd(str,days){
	 var date = new Date(str);
	 date.setDate(date.getDate() + days);
	 return date.Format("yyyy-MM-dd");
 }
 
 function initDatePicker(info,serverTime,timezonerawoffset,beforeNum,afterNum){
	 var noEnd = false;
	 if(typeof(info.noEnd) != "undefined" || info.noEnd == true){
		 noEnd = true;
		 $("#timeInputRoom").html("<input type='text' id='startTime' name='startTime' class='Timeinput' style='margin-left: 80px;'>");
	 }
	 if(typeof(info.NeedInit) == "undefined" || info.NeedInit == true){
		 var now = new Date();
		 now.setHours(0, 0, 0, 0);
		 var UTCDay = Date.UTC(now.getFullYear(),now.getMonth(),now.getDate());
		 var timezoneDate = new Date(UTCDay + (UTCDay - now.getTime()));
		 var beginDate = new Date(timezoneDate);
		 var days = info.days;
		 if(days){
			 beginDate.setDate(beginDate.getDate()-days);
		 }
		 beginDate = beginDate.Format("yyyy-MM-dd");
		 var showVal = beginDate;
		 $("#startTime").val(beginDate);
		  if(serverTime){
			  if(beforeNum != 0){
				  beforeNum = beforeNum || -1;
			  }
			var time = timeStrAdd(getRealServerTime(parseFloat(serverTime),timezonerawoffset),beforeNum);
			showVal = time;
			$("#startTime").val(time);
		 }
		 if(!noEnd){
			 var endDate = timezoneDate.Format("yyyy-MM-dd");
			 //结束日期可变
			 if(afterNum){
				 endDate = timeStrAdd($("#startTime").val(),afterNum);
			 }
			 $("#endTime").val(endDate);
			 showVal += '到' + $("#endTime").val();
		 }
		 $("#dateValue").text(showVal);
		 if(noEnd){
			 $(".calendar").css("padding-left","33px");
		 }
	 }
	 
	$("#startTime").datepicker({dateFormat:"yy-mm-dd"});
	if(!noEnd){
		$("#endTime").datepicker({dateFormat:"yy-mm-dd"});
	}
	$("#datePicker_a").click(function(){
		if($("#datePanel").css("display") == "block"){
			$("#datePanel").css({display:"none"});
		}else{
			$("#datePanel").css({display:"block"});
		}
	});
	$("#confirmBtn").click(function(){
		$("#datePanel").css({display:"none"});
		info.func();
		var showVal = $("#startTime").val();
		if(!noEnd){
			showVal += '到' + $("#endTime").val();
		}
		$("#dateValue").text(showVal);
	});
	$("#cancelBtn").click(function(){
		$("#datePanel").css({display:"none"});
	});
 }
 
 //绝对值转百分比
 function ChartVoToScaleDate(data,name){
	 result = [{data:[],type:'pie',name:name}];
	 for(var i = 0 ; i < data.length ; i++){
		 result[0].data.push([data[i].type.toString(),data[i].num]);
	 }
	 return result;
 }
 
 //时间类型转换
 function ChartVOtoDateData(data,dateStr,timezone){
	 result = {data:[],name:dateStr};
	 var maxSize = 0;
	 localZone = new Date().getTimezoneOffset() * 60000;
	 for(var i = 0 ; i < data.length ; i++){
		 var date = new Date(parseInt(data[i].addTime) - localZone);		//时区转换
		 date.setUTCFullYear(2000, 1, 1);
		 maxSize = Math.max(data[i].num.toString().length,maxSize);
		 result.data.push([date.getTime(),data[i].num]);
	 }
	 result.maxSize = maxSize;
	 return result;
 }

 Date.prototype.bfFormatter = function(){
	 var ret = '';
	 ret += this.getFullYear() + "-";
	 var month = this.getMonth() + 1;
	 ret += dateAddPrefix(month) + "-";
	 var day = this.getDate();
	 ret += dateAddPrefix(day) + " ";
	 
	 var hour = this.getHours();
	 ret += dateAddPrefix(hour) + ":";
	 var minute = this.getMinutes();
	 ret += dateAddPrefix(minute) + ":";
	 var seconds = this.getSeconds();
	 ret += dateAddPrefix(seconds);
	 return ret;
 }
 
 function dateAddPrefix(value){
	 if(value < 10){
		 return '0' + value;
	 }
	 return value;
 } 
 
 function getTimeZone(){
	 var now = new Date();
	 now.setHours(0, 0, 0, 0);
	 var UTCDay = Date.UTC(now.getFullYear(),now.getMonth(),now.getDate());
	 var timezone = UTCDay - now.getTime();
	 return timezone;
 }
 
 //-----------------------------跨服多区选择---------------------------
 function toSelMultiCrossArea(curPage, context){
		var numPerPage = 100;
		
		$.get(context + "/multicrossarea.do", {'curpage':curPage, 'countpp':numPerPage}, function(data, status){
			  var $dialog = $('#mulSelectCrossAreaDiv');
			  $dialog.find('.muliDisplayArea:first').html(data);
			  $dialog.css("position", "absolute");
			  var left = ($('body').width() - $dialog.width()) / 2;
			  $dialog.css("left", left + "px");
			  $dialog.show();
			  $dialog.draggable();
		});
	}

	function multiCrossAssure(){
		$('#mulSelectCrossAreaDiv').hide();
		if(isAllAreas()){
			$('.multiResultAllCrossArea').val('1');
		}else{
			$('.multiResultAllCrossArea').val('0');
			$('.multiResultCrossAreas').val(getSelectedCrossAreas());
		}
	}

	function multiCrossCancel(){
		$('#mulSelectCrossAreaDiv').hide();
	}

	var crossAreaCache = {};

	function multiChooseCrossArea(areaObj, areaId, targetSaverId, dialogId){
		var $dialog = $('#' + dialogId);
		var $targetSaver = $dialog.find('.' + targetSaverId + ':first');
		var value = $targetSaver.val();
		var isDefault = false;
		var $defaultSelectFlagObj;
		if($(areaObj).hasClass('defaultSelectArea')){
			isDefault = true;
			$defaultSelectFlagObj = $dialog.find('.defaultSelectFlag:first');
		}
		
		
		if($(areaObj).hasClass('areaSelected')){
			$(areaObj).removeClass('areaSelected');
			delete crossAreaCache[areaId];
			if(isDefault){
				$defaultSelectFlagObj.val('0');
			}
		}else{
			$(areaObj).addClass('areaSelected');
			crossAreaCache[areaId] = areaId;
			if(isDefault){
				$defaultSelectFlagObj.val('1');
			}
		}
		$targetSaver.val(value);
	}

	function allCrossSelectOrNot(obj){
		var $dialog = $('#mulSelectCrossAreaDiv');
		var $selectd = $(obj).filter(':checked');
		if($selectd.size() > 0){ //选中状态
			$dialog.find('a.multiSelectArea').each(function(){
				if(!$(this).hasClass('areaSelected')){
					$(this).click();
				}
			});
		}else{
			$dialog.find('a.multiSelectArea').each(function(){
				if($(this).hasClass('areaSelected')){
					$(this).click();
				}
			});
		}
	}
	//多选服务器参数重置
	function multiCrossAreaReset(){
		crossAreaCache = {};
		$('#mulSelectCrossAreaDiv .virginSet').val('');	
	}

	function multiCrossAreasFillParams(mapObj){
		if(isAllCrossAreas()){
			mapObj['crossall'] = true;
		}else{
			mapObj['crossareaList'] = getSelectedCrossAreas();
		}
	}

	function isMultiSelCrossValid(){
		if(isAllCrossAreas()){
			return true;
		}
		var count = 0;
		$.each(crossAreaCache,function(key,value){  
			count++;
			return false;
		});
		return count > 0;
	}

	function isAllCrossAreas(){
		return $("#mulSelectCrossAreaDiv input.allAreaOption:checked").size() > 0;
	}

	function getSelectedCrossAreas(){
		var ret = "";
		$.each(crossAreaCache,function(key,value){  
			if(ret.length > 0){
				ret = ret + ",";
			}
			ret = ret + key;
		});
		return ret;
	}

	function autoSelectSelectedCrossAreas(){
		var $multiResultAreas = $('.multiResultCrossAreas');
		if($multiResultAreas.size() > 0){
			var areas = $multiResultAreas.val();
			var start = 0;
			var splitIndex = -1;
			do{
			    splitIndex = areas.indexOf(',', start);
			    var subStr = ""; 
			    if(splitIndex == -1){
			    	subStr = areas.substring(start);
			    }else{
			    	subStr = areas.substring(start, splitIndex);
			    	start = splitIndex + 1;
			    }
			    
			    if(subStr != undefined && subStr.length > 0){
				    var id = parseInt(subStr);
				    crossAreaCache[id] = id;
			    }
			    
			}while(splitIndex != -1);
		}
		$.each(crossAreaCache,function(key,value){
			$('a.withId_' + key).addClass('areaSelected');
		});
	}
//-----------------------------跨服多区选择结束----------------------------
	
	//-----------------------------global多区选择---------------------------
	 function toSelMultiGlobal(curPage, context){
			var numPerPage = 100;
			
			$.get(context + "/multiglobal.do", {'curpage':curPage, 'countpp':numPerPage}, function(data, status){
				  var $dialog = $('#mulSelectGlobalDiv');
				  $dialog.find('.muliDisplayArea:first').html(data);
				  $dialog.css("position", "absolute");
				  var left = ($('body').width() - $dialog.width()) / 2;
				  $dialog.css("left", left + "px");
				  $dialog.show();
				  $dialog.draggable();
			});
		}

		function multiGlobalAssure(){
			$('#mulSelectGlobalDiv').hide();
			if(isAllAreas()){
				$('.multiResultAllGlobal').val('1');
			}else{
				$('.multiResultAllGlobal').val('0');
				$('.multiResultGlobals').val(getSelectedGlobals());
			}
		}

		function multiGlobalCancel(){
			$('#mulSelectGlobalDiv').hide();
		}

		var globalCache = {};

		function multiChooseGlobal(areaObj, areaId, targetSaverId, dialogId){
			var $dialog = $('#' + dialogId);
			var $targetSaver = $dialog.find('.' + targetSaverId + ':first');
			var value = $targetSaver.val();
			var isDefault = false;
			var $defaultSelectFlagObj;
			if($(areaObj).hasClass('defaultSelectArea')){
				isDefault = true;
				$defaultSelectFlagObj = $dialog.find('.defaultSelectFlag:first');
			}
			
			
			if($(areaObj).hasClass('areaSelected')){
				$(areaObj).removeClass('areaSelected');
				delete globalCache[areaId];
				if(isDefault){
					$defaultSelectFlagObj.val('0');
				}
			}else{
				$(areaObj).addClass('areaSelected');
				globalCache[areaId] = areaId;
				if(isDefault){
					$defaultSelectFlagObj.val('1');
				}
			}
			$targetSaver.val(value);
		}

		function allGlobalSelectOrNot(obj){
			var $dialog = $('#mulSelectGlobalDiv');
			var $selectd = $(obj).filter(':checked');
			if($selectd.size() > 0){ //选中状态
				$dialog.find('a.multiSelectArea').each(function(){
					if(!$(this).hasClass('areaSelected')){
						$(this).click();
					}
				});
			}else{
				$dialog.find('a.multiSelectArea').each(function(){
					if($(this).hasClass('areaSelected')){
						$(this).click();
					}
				});
			}
		}
		//多选服务器参数重置
		function multiGlobalReset(){
			globalCache = {};
			$('#mulSelectGlobalDiv .virginSet').val('');	
		}

		function multiGlobalsFillParams(mapObj){
			if(isAllGlobals()){
				mapObj['globalall'] = true;
			}else{
				mapObj['globalareaList'] = getSelectedGlobals();
			}
		}

		function isMultiSelGlobalValid(){
			if(isAllGlobals()){
				return true;
			}
			var count = 0;
			$.each(globalCache,function(key,value){  
				count++;
				return false;
			});
			return count > 0;
		}

		function isAllGlobals(){
			return $("#mulSelectGlobalDiv input.allAreaOption:checked").size() > 0;
		}

		function getSelectedGlobals(){
			var ret = "";
			$.each(globalCache,function(key,value){  
				if(ret.length > 0){
					ret = ret + ",";
				}
				ret = ret + key;
			});
			return ret;
		}

		function autoSelectSelectedGlobals(){
			var $multiResultAreas = $('.multiResultGlobals');
			if($multiResultAreas.size() > 0){
				var areas = $multiResultAreas.val();
				var start = 0;
				var splitIndex = -1;
				do{
				    splitIndex = areas.indexOf(',', start);
				    var subStr = ""; 
				    if(splitIndex == -1){
				    	subStr = areas.substring(start);
				    }else{
				    	subStr = areas.substring(start, splitIndex);
				    	start = splitIndex + 1;
				    }
				    
				    if(subStr != undefined && subStr.length > 0){
					    var id = parseInt(subStr);
					    globalCache[id] = id;
				    }
				    
				}while(splitIndex != -1);
			}
			$.each(globalCache,function(key,value){
				$('a.withId_' + key).addClass('areaSelected');
			});
		}
	//-----------------------------global多区选择结束----------------------------
	
//-----------------------------监控邮件开始-------------------------------
	function deleteEmailTemplate(){
		$('#mail_pop_error').html('');
		var $option = $('#mail_select_title option:selected');
		var id = $option.attr('value');
		if(id == -1){
			$('#mail_pop_error').html('请选择一个模板');
		}else{
			$.post($('#ctx').val() + "/monitor/monitor/deletemailtemplate.do", {'id':id}, function(data){
				if(data.id == -1){
					$('#mail_pop_error').html('删除失败， 错误码：' + data.title);
				}else{
					deleteMailTemplateToSelect(data);
				}
			});
		}
	}

	function modifyEmailTemplate(){
		$('#mail_pop_error').html('');
		var $option = $('#mail_select_title option:selected');
		var id = $option.attr('value');
		if(id == -1){
			$('#mail_pop_error').html('请选择一个模板');
		}else{
			var title = $('#mailtitle').val();
			var content = $('#mailcontent').val();
			if(title.length <= 0 || content.length <= 0){
				$('#mail_pop_error').html('邮件标题和内容不能为空');
			}else{
				$.post($('#ctx').val() + "/monitor/monitor/modifymailtemplate.do", {'id':id, 'title':title, 'content':content}, function(data){
					if(data.id == -1){
						$('#mail_pop_error').html('修改失败， 错误码：' + data.title);
					}else{
						updateMailTemplateToSelect(data);
					}
				});
			}
		}
	}

	function addEmailTemplate(){
		$('#mail_pop_error').html('');
		var title = $('#mailtitle').val();
		var content = $('#mailcontent').val();
		if(title.length <= 0 || content.length <= 0){
			$('#mail_pop_error').html('邮件标题和内容不能为空');
		}else{
			$.post($('#ctx').val() + "/monitor/monitor/createmailtemplate.do", {'title':title, 'content':content}, function(data){
				if(data.id == -1){
					$('#mail_pop_error').html('增加失败， 错误码：' + data.title);
				}else{
					addMailTemplateToSelect(data, true);
				}
			});
		}
	}
//------------------------------------------------------------------------------------------

//---------------------常用查询-------------------------
function commonUseQuery(obj){
	var $container = $(obj).closest('div');
	var checkOk = true;
	$container.find('.require').each(function(){
		var $param = $(this).next('.param');
		if(!$param.val()){
			checkOk = false;
			alert($(this).html() + '不能为空');
			return false;
		}
		return true;
	});
	if(!checkOk){
		return;
	}
	
	var sql = $container.find('span:first').html();
	var $sql = $('<span>' + sql + '</span>');
	var $params = $container.find('.param');
	$params.each(function(){
		var value = $(this).val();
		if(!value){
			$sql.find('.' + $(this).attr('name')).remove();
		}
	});
	sql = $sql.text();
	if(sql.indexOf('$logtime') != -1){
		var timeStr = $container.find('.logtime').val();
		if(!timeStr){
			alert('请输入查询时间');
			return;
		}
		timeStr = timeStr.substring(0, 'yyyy-MM-dd'.length);
		var timearray = timeStr.split('-');
		var replacement = timearray[0] + timearray[1] + timearray[2];
		sql = sql.replace('$logtime', replacement);
	}
	$params.each(function(){
		sql = sql.replace('$' + $(this).attr('name'), $(this).val());
	});
	
	var currenturl = $('#currenturl').val();
	var url = $('#ctx').val() + '/query/db/query.do';
	var mode = $('#mode').val();
	$.post(url, {"sql" : sql, "mode" : mode, "preurl":currenturl}).success(function(data) {
		$(".content_main").html(data);
	}).error(function() { 
		alert("服务器离家出走了，请联系研发人员...");
	});
}

function registerCommonQueryButton(){
	$('.commonquery').click(function(){
		commonUseQuery(this);
	});
}

//--------------------服务器配置下载----------------------
function downloadConfig(serverType, worldId){
	/*var $downloadForm = $('#downloadForm');
	$downloadForm.find('input[name=servertype]').val(serverType);
	$downloadForm.find('input[name=worldid]').val(worldId);
	$downloadForm.submit();*/
	var url = $('#ctx').val() + '/ps/area/downloadconfig.do?servertype=' + serverType + '&worldid=' + worldId;
	//window.open(url,'下载','height=100,width=400,top=500,left=500,toolbar=no,menubar=no,scrollbars=no,resizable=no,location=no,status=no')
	window.open(url);
//	$.get($('#ctx').val() + '/ps/area/downloadconfig.do', {'servertype':serverType, 'worldid':worldId}, function(data){
//	});
}

//detailview 全部展开 或 全部收回
function toggleExpand(obj){
	var $container = $(obj).closest('.datagrid-view');
	if($(obj).hasClass('datagrid-row-expand')){
		$container.find('.datagrid-row-expander').each(function(){
			if(this == obj){
				$(obj).removeClass('datagrid-row-expand');
				$(obj).addClass('datagrid-row-collapse')
			}else if($(this).hasClass('datagrid-row-expand')){
				$(this).click();
			}
		});
	}else{
		$container.find('.datagrid-row-expander').each(function(){
			if(this == obj){
				$(obj).removeClass('datagrid-row-collapse');
				$(obj).addClass('datagrid-row-expand')
			}else if($(this).hasClass('datagrid-row-collapse')){
				$(this).click();
			}
		});
	}
}


function registerBfSelect(){
	$('.chooseRegionTrigger').click(function(){
		if($('.optionContainer:hidden').size() > 0){
			$('.optionContainer').css('left', $(this).position().left + 'px');
			$('.optionContainer').css('top', ($(this).position().top + $(this).height()) + 'px');
			$('.optionContainer').show();
		}else{
			$('.optionContainer').hide();	
		}
	});
	
	$('.optionContainer .item').mouseover(function(){
		$(this).addClass('itemmouseover').siblings().removeClass('itemmouseover');
	}).mouseout(function(){
		$(this).removeClass('itemmouseover');
	}).click(function(){
		$('.chooseRegionTrigger .choosedisplay').html($(this).html());
		$(this).attr('selected', 'selected').siblings().removeAttr('selected');
		$('.optionContainer').hide();
	});
}
function queryAreaChargeStat(url,para,processData,queryAreaId){
	queryAreaId = queryAreaId || 'queryAreaChargeStat';
	var areaId = $('#'+queryAreaId).parent().find('.optionContainer .item[selected=selected]').attr('value');
	params.areaId = areaId;
	$.ajax({url:url,data:params,
			success:function(data){
				processData(data);
			}
		});
}

function showDiv(isShow,id){
	id = id || 'condition_area_div';
	if(isShow){
		$('#'+id).show();
	}
	else{
		$('#'+id).hide();
	}
}

function getQueryAreaId(num){
	var areaId = $("#search_type_btn").attr("value");
	if(num && num > 0){
		areaId = $("#search_type_btn"+num).attr("value");
	}
	return areaId;
}

function getDateDiff(date1,date2){
	var temp = "-";
	var strDateSArray = date1.split(temp);
	var strDateEArray = date2.split(temp);
	var strDateS = new Date(strDateSArray[0],strDateSArray[1],strDateSArray[2]);
	var strDateE = new Date(strDateEArray[0],strDateEArray[1],strDateEArray[2]);
	return parseInt(Math.abs(strDateS - strDateE ) / 1000 / 60 / 60 /24);
}

function formatMin(min){
	var str = '';
	if(min<60){
		str += min+"分钟";
	}
	var hour = Math.floor(min/60);
	var day = Math.floor(min/60/24);
	var year = Math.floor(min/60/24/365);
	if(year>0){
		str = year +"年";
	}
	if(day>0){
		if(year>0){
			day = Math.floor(min/60/24%365);
			if(day>0){
				str += day + "天";
			}
		}else{
			str += day + "天";
		}
	}
	if(hour>0){
		if(year>0 || day>0){
			hour = Math.floor(min/60%24);
			if(hour>0){
				str += hour + "小时";
			}
		}
		else{
			str += hour + "小时";
		}
	}
	if(year>0 || day>0 || hour>0){
		min = Math.floor(min%60);
		if(min>0){
			str += min + "分钟";
		}
	}
	return str;
}
$.extend($.fn.datagrid.methods, {
	editCell: function(jq,param){
		return jq.each(function(){
			var opts = $(this).datagrid('options');
			var fields = $(this).datagrid('getColumnFields',true).concat($(this).datagrid('getColumnFields'));
			for(var i=0; i<fields.length; i++){
				var col = $(this).datagrid('getColumnOption', fields[i]);
				col.editor1 = col.editor;
				if (fields[i] != param.field){
					col.editor = null;
				}
			}
			$(this).datagrid('beginEdit', param.index);
			for(var i=0; i<fields.length; i++){
				var col = $(this).datagrid('getColumnOption', fields[i]);
				col.editor = col.editor1;
			}
		});
	}
});

function selectListInit(params){
	$('.mod_select_down a.btn_select_outline').click(function(){
		var $listAreas = $(this).next('ul.btn_select_outline');
		if($listAreas.css('display') == 'block'){
			$listAreas.hide();
		}else{
			$listAreas.show();
		}
	});
	$('.mod_select_down a.btn_select_outline').next('ul.btn_select_outline').find('li').click(function(){
		var $lia = $(this).find('a');
		var value = $lia.attr('value');
		var displayValue = $lia.html();
		//alert($lia.size() + '  ' + value + '  ' + displayValue);
		$(this).parent().prev().attr('value', value).children('span').html(displayValue);
		
		if(params && params.listChoose){
			params.listChoose($lia);
		}
		$(this).parent().hide();
	});
}
function postExportFileData(ctx,line,type,filename){
	Highcharts.post(ctx+'/stat/exportfile/file.do',{
        data: line,
        type: type,
        fileName:filename
      });
}

function selectServerList(callback){
	$("#search_type_btn").bind("click",function(){
		$('#search_type_list_div').toggle();
	});
	$("#search_type_list").children("li").each(function() {
	    $(this).bind("click",function(){
	    	$(this).parent().parent().prev().children("span").html($(this).text());
	    	$('#search_type_list_div').toggle();
	    	$("#search_type_btn").attr("value",$(this).children("a").attr("value"));
	    	if(callback){
	    		callback($(this).children("a").attr("value"));
	    	}
	    });
	});
	$('#search_type_list_div').parent().bind('mouseleave',function(){
		$('#search_type_list_div').hide();
	});
	$("#btn_search").bind("click",function(){
		refreshInfo();
	});
}

function initBfSelect(bfSelect){
	var selector = bfSelect.selector;
	var data = bfSelect.data;
	if(data){
		if(bfSelect.topData){
			for(var i = 0; i < bfSelect.topData.length; i++){
				data.unshift(bfSelect.topData[i]);
			}
		}
		var initId = data[0].id; var initName = data[0].name;
		if(bfSelect.initId){
			for(var i = 0; i < data.length; i++){
				var dt = data[i];
				if(dt.id == bfSelect.initId){
					initId = bfSelect.initId;
					initName = dt.name;
					break;
				}
			}
		}
		var text = '<div class="mod_select_down">';
		text += '<a href="javascript:void(0);" onclick="return false;" id="search_type_btn" class="btn_white_2 btn_select_outline" value="' + initId + '"><span>';
		text += initName;
		text += '</span><i class="ico"></i></a>';
		text += '<div id="search_type_list_div">';
		text += '<ul class="down_list btn_select_outline" id="search_type_list" style="position:static;border:none">';
		for(var i = 0; i < data.length; i++){
			var dt = data[i];
			text += '<li><a href="javascript:void(0);" onclick="return false;" _search_type="1" value="' + dt.id + '">' + dt.name + '</a></li>';
		}
	    text += '</ul></div></div>';
	    $(selector).html(text);
	    bfSelect.selectCallback(initId);
	    selectServerList(bfSelect.selectCallback); 
	}else if(!bfSelect.loadDataFlag && bfSelect.loadData){
		$.get(bfSelect.loadData, bfSelect.loadParams, function(data){
			bfSelect.loadDataFlag = true;
			bfSelect.data = data;
			initBfSelect(bfSelect);
		});
	}
	
}

function selectItemList(itemRequest){
	$("#search_type_btn").bind("click",function(){
		$('#search_type_list').toggle();
		$("#search_type_list").children("li").each(function(index) {
			if($("#search_type_btn").attr("value") == $(this).children("a").attr("value")){
				$("#search_type_list").scrollTop(index*30);
			}
		});
	});
	$("#search_type_list").children("li").each(function() {
	    $(this).bind("click",function(){
	    	$(this).parent().prev().children("span").html($(this).text());
	    	$('#search_type_list').toggle();
	    	$("#search_type_btn").attr("value",$(this).children("a").attr("value"));
	    	itemRequest($(this).children("a").attr("value"));
	    });
	});
	$('#search_type_list').parent().bind('mouseleave',function(){
		$('#search_type_list').hide();
	});
}
function selectItemList2(number,itemRequest){
	var num = '';
	if(!isNaN(number)){
		num = number;
	}
	$('#search_type_list_div'+num).hide();
	$("#search_type_btn"+num).bind("click",function(){
		$('#search_type_list_div'+num).toggle();
		$("#search_type_list"+num).children("li").each(function(index) {
			if($("#search_type_btn"+num).attr("value") == $(this).children("a").attr("value")){
				$("#search_type_list_div"+num).scrollTop(index*30);
			}
		});
	});
	$("#search_type_list"+num).children("li").each(function(index) {
	    $(this).bind("click",function(){
	    	$(this).parent().parent().prev().children("span").html($(this).text());
	    	$('#search_type_list_div'+num).toggle();
	    	$("#search_type_btn"+num).attr("value",$(this).children("a").attr("value"));
	    	if(itemRequest){
	    		itemRequest($(this).children("a").attr("value"));
	    	}
	    });
	});
	$('#search_type_list_div'+num).parent().bind('mouseleave',function(){
		$('#search_type_list_div'+num).hide();
	});
}

function getRealServerTime(time,serverTimezonerawoffset){
	time += parseFloat(serverTimezonerawoffset) + (new Date().getTimezoneOffset() * 60*1000);
	return time;
}

function parseDateTime(strTime){
	var date = strTime.split(' ');
	var dateArray = date[0].split('-');
	var timeArray = date[1].split(':');
	var now = new Date(dateArray[0],dateArray[1],dateArray[2]);
	now.setHours(timeArray[0]);
	now.setMinutes(timeArray[1]);
	now.setSeconds(timeArray[2]);
	return now;
}


function configTimer(){
	id:0
}

/**
 * 显示模态对话框提交表单方法
 * @param url
 * @param para
 * @param callback
 * @param method
 */
function submitFormModal(url,para,callback,method){
	method = method || 'POST';
	$.ajax({url:url,data:para,method:method,success:function(data) {
		callback(data);
	  },beforeSend:function() {
			 var h = document.body.clientHeight; 
			 $("<div class=\"datagrid-mask\"></div>").css({display:"block",width:"100%",height:h}).appendTo("body"); 
			 $("<div class=\"datagrid-mask-msg\"></div>").html("正在处理中，请稍候。。。").appendTo("body").css({display:"block",
			  left:($(document.body).outerWidth(true) - 190) / 2,
			  top:(h - 45) / 2});
		},
		complete:function(data) {
			   $('.datagrid-mask-msg').remove();
			   $('.datagrid-mask').remove();
			 }});
}

function getUrl(url){
	$.get(url, function(data){
		$('#formDiv').html(data);
	});
}


function postExcelData(ctx,fileName,headArray,data){
	if(!data){
		return;
	}
	var line ='';
	for(var i=0;i<headArray.length;i++){
		var ret = '';
		var obj = headArray[i];
		for(var key in obj){
			ret = obj[key];
		}
		if(line == ''){
			line = ret;
		}
		else{
			line += ','+ret;
		}
	}
	for(var i =0;i<data.length;i++){
		var obj = data[i];
		var temp = '';
		for(var j=0;j<headArray.length;j++){
			var headObj = headArray[j];
			for(var key in headObj){
				if(temp == ''){
					temp = obj[key];
				}
				else{
					temp += ","+obj[key];
				}
			}
		}
		if(line == ''){
			line = temp;
		}
		else{
			line += ";"+temp;
		}
	}
	Highcharts.post(ctx+'/stat/exportfile/file.do',{
        data: line,
        type: 'csv',
        fileName:fileName
      });
}

function postGridExcelData(ctx,datagrid,fileName){
	var datagrObj = $('#'+datagrid);
	var dataGridData = datagrObj.datagrid('getData');
	var opts = datagrObj.datagrid('options');
	var headArray = [];
	var tempArray = opts.columns;
	for(var i=0;i<tempArray.length;i++){
		var objArray = tempArray[i];
		for(var j=0;j<objArray.length;j++){
			var obj = objArray[j];
			var head = {};
			head[obj.field] = obj.title;
			headArray.push(head);
		}
	}
	postExcelData(ctx,fileName,headArray,dataGridData.rows);
}
function postGridExcelData(ctx,datagrid,data,fileName){
	var datagrObj = $('#'+datagrid);
	var opts = datagrObj.datagrid('options');
	var headArray = [];
	var tempArray = opts.columns;
	for(var i=0;i<tempArray.length;i++){
		var objArray = tempArray[i];
		for(var j=0;j<objArray.length;j++){
			var obj = objArray[j];
			var head = {};
			head[obj.field] = obj.title;
			headArray.push(head);
		}
	}
	postExcelData(ctx,fileName,headArray,data);
}
