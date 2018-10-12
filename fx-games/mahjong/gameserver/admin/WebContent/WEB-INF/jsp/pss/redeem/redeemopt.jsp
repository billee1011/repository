<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%request.setAttribute("ctx", request.getContextPath());%>
<div id="content_area" style="overflow: auto; vertical-align: top;height:700px;">
      <div class="to_redeem_div" style="width: 53%; display: inline-block;">
         <div><h3><span id="mail_pop_error" class="tip_error"></span></h3></div>
         <div>
           <span class="tip">标题：</span><input id="mailtitle" name="mailtitle" class="inputlength"/>
  <span class="tip">选择邮件模板：</span><select id="mail_select_title" class="inputlength mail_select_title" ><option value="-1">无</select>
        <button class="btn btn_blue" onclick="modifyMailTemplate()">修改</button>
        <button class="btn btn_blue" onclick="addMailTemplate()">增加</button>
  <button class="btn btn_blue" onclick="deleteMailTemplate()">删除</button>
  <button class="btn btn_blue" onclick="toSelMultiArea(1, '${ctx}')">选服</button>
</div>
<div>
   <span class="tip" style="vertical-align: top;">内容：</span><textarea id="mailcontent" name="mailcontent" rows="10" cols="110" style="width: auto;"></textarea>
</div>
<div>
   <span class="tip">角色类型：</span>
   <select name="role_type" class="inputlength">
     		    <option value="0">部分玩家</option>
     		    <option value="1">所有玩家</option>
     <!-- 		    <option value="2">所有新玩家</option> -->
     		</select>
     		<button id="to_batch_add" class="btn btn_blue" onclick="popUp('batch_players');">批量</button>
</div>
<div>
    <span class="tip" style="margin-left: 2px;">帐号：</span>
    <input id="role_id" name="role_id" class="inputlength"/>
    <button class="btn btn_blue" title="添加角色" onclick="addRoleToTable()">添加</button>
</div>
<div>
    <span class="tip" style="margin-left: 2px;">角色名：</span>
    <input id="role_name" name="role_name" class="inputlength"/>
    <span class="tip" style="margin-left: 2px;">钻石：</span>
    <input id="redeem_diamond" name="redeem_diamond" class="intonly"/>(注：钻石小于等于0，就是发邮件)
    <span id="role_error" class="tip_error"></span>
</div>
<div>
   <!-- 角色表格 -->
   <div style="display:inline-block;float:left;">
     <table id="role_display_head_table" class="displaying">
        <thead>
        <tr class="title"><th>帐号</th><th>角色名</th><th style="border-right: none;">操作</th><th style="width:10px;border-left: none;"></th></tr>
        </thead>
     </table>
     <div class="display_body_div">
     <table id="role_display_body_table" class="displaying">
       <thead>
        <tr class="title"><th>帐号</th><th>角色名</th><th>操作</th></tr>
        </thead>
        <tbody>
        <tr><td></td><td></td><td></td></tr>
        <tr><td></td><td></td><td></td></tr>
        <tr><td></td><td></td><td></td></tr>
        <tr><td></td><td></td><td></td></tr>
        <tr><td></td><td></td><td></td></tr>
        <tr><td></td><td></td><td></td></tr>
        <tr><td></td><td></td><td></td></tr>
        </tbody>
     </table>
     </div>
   </div>
</div>
<input type="hidden" id="redeem_type" value="" />
      </div>
<input type="hidden" id = "multiResultPlace" value="0"/>
 <div style="padding-left: 20px;">
    <button id="redeem_commit" class="btn btn_blue" onclick="redeemCommit()">提交</button>
    <button id="redeem_reset" class="btn btn_blue" onclick="redeemReset()">重置</button>
</div>
  </div>
<script type="text/javascript">
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
	  	
	  	$('#selected_itemid').keyup(function(){
	  		selectItemIdKeyup();
	  	});
	  	
	  	onlyInputInt();
   });
</script>