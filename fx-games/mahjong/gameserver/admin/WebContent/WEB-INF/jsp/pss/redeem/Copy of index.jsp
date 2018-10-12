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
    height: 30px;
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
	<h2>补偿管理</h2>
</div>
<input id="ctx" type="hidden" value="${ctx }">
<div class="bd" style="padding-top: 10px;margin-bottom: 10px;">
     
</div>

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
<div id="item_templates" class="pop_layer_v2" style="display:none;">
    <div class="pop_layer_main to_redeem_div" style="border: none;">
        <div class="pop_layer_title">
		   <h3>道具/装备模板
		   <span id="item_templates_error" class="tip_error"></span>
		   </h3>
		</div>
		<div class="pop_layer_cont">
		  <div>
		      <span class="tip">道具/装备搜索：</span>
		      <input class="inputlength" onkeyup="listItems(this);"/>
		  </div>
		   <div>
			   <span class="tip">道具/装备列表：</span>
			   <select id="item_choose_list" class="inputlength" size="10"></select>
			   <button class="btn btn_blue" onclick="chooseItem()">选择</button>
			   <button class="btn btn_blue" onclick="cancelPop('item_templates')">取消</button>
		   </div>
		</div>
    </div>
</div>
<div id="redeemResult" class="pop_layer_v2" style="display:none;">
	<div class="pop_layer_main" style="border: none;">
        <div class="pop_layer_title">
		   <h3>补偿结果
		   </h3>
		</div>
		<div class="pop_layer_cont">
		   <span id="redeemResultMsg" class="tip"></span>
		   <div class="redeemResultError">
		   
		   </div>
		   <div>
		       <button class="btn btn_blue" onclick="cancelPop('redeemResult')">确定</button>
		   </div>
		</div>
    </div>
</div>
<!-- 
 <div id="mail_operation" class="pop_layer_v2" style="display:none;"> 
	<div id="mail_operation" class="pop_layer_main to_redeem_div">
		<div class="pop_layer_title">
		   <h3>邮件模板管理
		   <span id="mail_pop_error" class="tip_error"></span>
		   </h3>
		</div>
	    <div class="pop_layer_cont">
	       <div>
	        <span class="tip">邮件选择：</span><select id="mail_select" class="inputlength mail_select_title"></select>
	        <button class="btn btn_blue">删除</button>
	        <button class="btn btn_blue">修改</button>
	        <button class="btn btn_blue" onclick="addMailTemplate()">增加</button>
	        <button class="btn btn_blue" onclick="cancelPop('mail_operation')">取消</button>
	        </div>
	        <input type="hidden" name="mailid" value="-1"/>
	        <div>
	        <span class="tip">邮件标题：</span>
	        <input name="mailtitle" class="inputlength"/>
	        </div>
	        <div>
	        <span class="tip">邮件内容：</span>
	        <textarea name="mailcontent" rows="10" cols="110" style="width: auto;"></textarea>
	        </div>
	    </div>
	</div>
</div>
 -->
<div id="formDiv" style="display:none;" class="pop_layer_v2 to_redeem_div">
</div>
<%@include file="../../mutiselectarea.jsp" %>
<script type="text/javascript">
mailCache = '';
batchPlayersTr = '<tr class="filledTr"><td>{}</td><td>{}</td><td>{}</td></tr>';

itemTr = '<tr class="filledTr"><td>{}</td><td>{}</td><td>{}</td><td>{}</td><td>{}</td></tr>';

commonDelete = '<a href=\"javascript:;\" onclick=\"removeFromTable(this)\">删除</a>';

    $(function(){
    	calScrollTableWidth('role_display_head_table', 'role_display_body_table');
    	calScrollTableWidth('item_display_head_table', 'item_display_body_table');
    	
    	fillMailTemplates();
    	$('#mail_select_title').change(function(){
    		mailSelectChange();
    	});
    	
    	$('#batch_players_upload').change(function(){
    		$('#upfileDisplay').attr('value', $(this).val());
    	});
    	
    	batchPendingPlayersUpload();
    	
    	//itemTemplates
    	fillItemTemplates();
    	
    	$('#selected_itemid').keyup(function(){
    		selectItemIdKeyup();
    	});
    	
    	onlyInputInt();
    	
    	$.get('${ctx}/pss/redeem/redeemrecord.do', function(data){
    		$('#recordContainer').html(data);
    	});
    });
    
    function listItems(obj){
    	var value = $(obj).val();
    	$('#item_choose_list option').each(function(){
    		if(!value){
    			$(this).show();
    		}else{
    			if($(this).text().indexOf(value) != -1){
    				$(this).show();
    			}else{
    				$(this).hide();
    			}
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
    
    //redeemCommit 等函数挪到了baofeng.js
</script>