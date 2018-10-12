<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%request.setAttribute("ctx", request.getContextPath());%>
<link href="${ctx}/css/css.css" rel="stylesheet" media="screen">
<script type="text/javascript" src="${ctx}/js/game/item.js"></script>
<style type="text/css">
<!--
.file{ filter:alpha(opacity:0);opacity: 0;}
.record_title{background-image: url(${ctx}/images/manage_center/keys.png);}
.datagrid-row{
    height: 25px;
}
.datagrid-cell,
.datagrid-cell-group,
.datagrid-header-rownumber,
.datagrid-cell-rownumber {
   font-size: 14px;
}

-->
</style>
<div class="title_area">
	<h2>玩家补偿</h2>
</div>
<input id="ctx" type="hidden" value="${ctx }">
<div class="bd" style="padding-top: 10px;margin-bottom: 10px;">
     <div id="tabs" class="easyui-tabs" style="height: auto;">
		<div title="补偿">
		</div>
		<div title="历史记录">
		   <div style="width: 100%; display: inline-block;">
		      <div class="record_title">
		             您好，你所有的操作将会被记录！
		      </div>
		    </div>
		</div>
	</div>
</div>
<div id="Container"></div>
<div id="mask_div" class="ui_mask" style="width: 1920px; height: 1746px; position: absolute; top: 0px; left: 0px; display: none; background-color: rgb(0, 0, 0); opacity: 0.6; z-index: 999; background-position: initial initial; background-repeat: initial initial;"></div>
<div id="batch_players" class="pop_layer_v2" style="display:none;">
    <div class="pop_layer_main to_redeem_div" style="border: none;">
        <div class="pop_layer_title">
		   <h3>上传角色
		   <span id="batch_players_pop_error" class="tip_error"></span>
		   </h3>
		</div>
		 <div class="pop_layer_cont">
		    <div><p>文件格式支持纯文本， 格式为每行：   userid&nbsp;&nbsp;&nbsp;&nbsp;rolename</p></div>
		    <div>
		       <span id="progress" class="progress-bar" style="background-color: #00FF00;"></span>
		    </div>
		    <div class="file-box">
		     <span class="tip">文件：</span>
		     <input id="upfileDisplay" class="inputlength" readonly="readonly"/>
		     <input type='button' id="batch_scan" class='btn btn_blue' value='浏览...' />
		     <input type="file" id="batch_players_upload" name="players" class="file"/>
		     <input type="submit" id="uploadsubmit" name="submit" style="margin-left: 10px;" class="btn btn_blue" value="上传" />
		     <button class="btn btn_blue" onclick="$('#batch_players').hide()">取消</button>
		     </div>
		 </div>
    </div>
</div>

<div id="formDiv" style="display:none;" class="pop_layer_v2 to_redeem_div">
</div>
<%@include file="../../mutiselectarea.jsp" %>
<script type="text/javascript">
mailCache = '';
batchPlayersTr = '<tr class="filledTr"><td>{}</td><td>{}</td><td>{}</td></tr>';

itemTr = '<tr class="filledTr"><td>{}</td><td>{}</td><td>{}</td><td>{}</td><td>{}</td><td>{}</td></tr>';

commonDelete = '<a href=\"javascript:;\" onclick=\"removeFromTable(this)\">删除</a>';

var $itemSource;
    $(function(){
    	initTabs();
    	//itemTemplates
	  	fillItemTemplates();
	  	$itemSource = $('#item_choose_list option').clone(true);
    });
    
    function listItems(obj){
    	var value = $(obj).val();
    	
    	$('#item_choose_list').empty();
    	
    	$itemSource.each(function(){
    		var add = false;
    		if(!value){
    			add = true;
    		}else if($(this).text().indexOf(value) != -1){
   				add = true;
    		}
    		if(add){
    			$(this).clone().appendTo('#item_choose_list');
    		}
    	});
    }
    
    function fillItemTemplates(){
    	var itemCache = genItems;
    	for(var i = 0; i < itemCache.length; i++){
    		var itemTemplate = itemCache[i];
    		$('<option value=\"' + itemTemplate.id + '\">' + itemTemplate.name + '</option>').appendTo($('#item_choose_list'));
    	}
    }
    
    //填充邮件模板选择
    function fillMailTemplates(){
    	mailCache = ${mailTemplateList};
    	for(var i = 0; i < mailCache.length; i++){
    		var mailTemplate = mailCache[i];
    		addMailTemplateToSelect(mailTemplate, false);
    	}
    }
    
    //function showRedeemOpt(obj){
    //	var content = $(obj).html();
    //	if(!content){
    //		$(obj).load('${ctx}/pss/redeem/redeemopt.do');
    //	}
    //}
    function showRedeemOpt(obj){
    	$('#Container').html('');
    	$('#Container').load('${ctx}/pss/redeem/redeemopt.do');
    }
    function showSendMail(obj){
    	$('#Container').html('');
    	$('#Container').load('${ctx}/pss/redeem/sendMail.do');
    }
    
    function showRedeemRecord(obj){
    	$('#Container').html('');
    	$('#Container').load('${ctx}/pss/redeem/redeemrecord.do');
    }
    
    function redeemmailrecord(obj){
    	$('#Container').html('');
    	$('#Container').load('${ctx}/pss/redeem/redeemmailrecord.do');
    }
    function initTabs(){
    	$("#tabs").tabs({
    		border:true,
    		tabWidth:200,
    		tabHeight:40,
    		onSelect:function(title){
    			var selectTabPanel = $('#tabs').tabs('getSelected'); 
				if('补偿' == title){
					showRedeemOpt(selectTabPanel[0]);
				}else if('历史记录'== title){
					showRedeemRecord(selectTabPanel[0]);
				}else if('邮件'== title){
					showSendMail(selectTabPanel[0]);
				}else if('邮件历史记录'==title){
					redeemmailrecord(selectTabPanel[0]);
				}
    		}
    		});
    }
    
    //redeemCommit 等函数挪到了baofeng.js
</script>