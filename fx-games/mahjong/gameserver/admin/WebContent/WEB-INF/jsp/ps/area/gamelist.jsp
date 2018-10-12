<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%request.setAttribute("ctx", request.getContextPath());%>
<style type="text/css">
<!--
.btn_white_2 {
border: 1px solid #c0c4cd;
border-radius: 3px;
background: #fff;
color: #0071ce;
}
.mod_select_down a:hover{
    background-color: #e2e2e2;
}
-->
</style>
<div class="title_area">
	<h2>游戏服管理</h2>
</div>
<input type="hidden" id="ctx" value="${ctx }"/>
<div class="bd" style="padding-top: 10px;">
    <div class="search">
        <div class="mod_select_down" style="display: inline-block; vertical-align: top;">
             <a href="javascript:void(0);" onclick="return false;" id="search_type_btn" class="btn_white_2 btn_select_outline" value="0"><span>服务器ID</span><i class="ico"></i></a>
             <ul class="down_list btn_select_outline" id="search_type_list" style="display: none;">
                 <li><a href="javascript:void(0);" onclick="return false;" class="exclude" value="0" targetPlaceholder="请输服务器ID">服务器ID</a></li>
                 <li><a href="javascript:void(0);" onclick="return false;" class="exclude" value="1" targetPlaceholder="请输服务器IP">服务器IP</a></li>
                 <li><a href="javascript:void(0);" onclick="return false;" class="exclude" value="2" targetPlaceholder="游戏区ID">游戏区ID</a></li>
             </ul>
        </div>
        <div style="display: inline-block;">
	        <span class="text_area"><textarea id="ipsearchinput" style="color:#333;" placeholder="请输服务器ID"></textarea></span> 
			<button type="submit" class="searchbutton"></button>
		</div>
		<div style="display: inline-block; vertical-align: top;">
			<!-- <button class="btn btn_blue" onclick="exportExcel()" style="margin-top:0px;">导出Excel</button> -->
		</div>
    </div>
	<div class="bd_title">
		<div class="bd_title_btns">
			<a id="btn_rename" href="<c:url value="/ps/area/create.do"/>" style="display: none;" class="btn btn_blue" title="增加一个实例"><span>新增</span></a>
			<a id="btn_back" href="javascript:void(0);" style="display: none;" onclick="return false;" class="btn btn_disable_v2" title="请选择一个或多个实例"><span>退还</span></a>
			<a id="btn_project" href="javascript:void(0);" onclick="return false;" class="btn btn_disable_v2" title="请选择一个或多个实例" style="display: none;"><span>分配至项目</span></a>
		</div>
		<div class="bd_title_op">
			<a id="btn_refresh" href="javascript:void(0);" onclick="return false;" class="ico ico_refresh" title="刷新当前页面"><span class="visually_hidden">刷新</span></a>
			<a id="btn_setting" href="javascript:void(0);" onclick="return false;" class="ico ico_setting" title="设置列表显示字段"><span class="visually_hidden">设置</span></a>
		</div>
	</div>
                            
	<div class="table_area" style="overflow: auto;">
		<div class="table_mask" id="cont_table_area" style="width: 100%;">
		  <c:choose>
		    <c:when test="${ areaListSize le 0}">
			  <div class="table_blank">
				<p class="text">还没有区服</p>
		      </div>
			</c:when>
			<c:otherwise>
				<div>
				    <input type="hidden" class="hiddenEditingIndex"/>
					<table id="areaListTable" style="height: 640px;">
					<thead style="height: 31px">
						<tr>
							<th data-options="field:'worldId',width:calColumnWidth(0.05)" title="研发商分配的世界ID">官方ID</th>
							<th data-options="field:'worldName',width:calColumnWidth(0.05)" title="研发商分配的名称">服务器名</th>
							<th data-options="field:'followerId',width:calColumnWidth(0.05)" title="主机leader的ID">主机</th>
							<th data-options="field:'platformName',width:calColumnWidth(0.05)" title="主机leader的ID">平台</th>
							<th data-options="field:'areaId',width:calColumnWidth(0.05)" title="平台分配的ID">游戏区ID</th>
							<th data-options="field:'areaName',width:calColumnWidth(0.08)" title="平台分配的ID">游戏区名</th>
							<th data-options="field:'adminAddress',width:calColumnWidth(0.1)">后台地址</th>
							<th data-options="field:'gameAddress',width:calColumnWidth(0.18)">游戏地址</th>
							<th data-options="field:'openTime',width:calColumnWidth(0.12),
							     editor:{
			                           type:'bfDateEdit',
			                           options:{
			                               savecallback:'dateSaveCallback',
			                           }
			                       }
							">开服时间</th>
							<th data-options="field:'combineTime',width:calColumnWidth(0.12),
							     editor:{
			                           type:'bfDateEdit',
			                           options:{
			                               savecallback:'dateSaveCallback2',
			                           }
			                       }
							">合服时间</th>
							<th data-options="field:'restartTime',width:calColumnWidth(0.12)">起服时间</th>
							<th data-options="field:'status',width:calColumnWidth(0.05),
							formatter:function(value,row,index){
							    return $('#hideStatusDiv .image_' + value).html();
							}">运行</th>
							<th data-options="field:'configFile',width:calColumnWidth(0.04), formatter:function(value,row,index){
							    return '<a href=javascript:; onclick=downloadConfig(' + 1 + ',' + row.worldId + ')>下载</a>';
							}">配置</th>
							<th data-options="field:'version',width:calColumnWidth(0.18), formatter:function(value,row,index){
							    var ret = '';
							    var serverVersion = row['serverVersion'];
							    var dataVersion = row['dataVersion'];
							    if(serverVersion){
							        ret += '服务器: ' + serverVersion;
							        if(dataVersion){
							            ret += '<br/>';
							        }
							    }
							    if(dataVersion){
							         ret += '策划数据: ' + dataVersion;
							    }
							    return ret;
							}">版本</th>
							<th data-options="field:'serverVersion',hidden:true"></th>
							<th data-options="field:'dataVersion',hidden:true"></th>
							<!-- <th style="width:210px;"><div class="op"><span class="table_title">操作</span></div></th> -->
						</tr>
					</thead>
						<tbody>
						</tbody>
					</table>
				  </div>
				</c:otherwise>
			</c:choose>
        </div>
    </div>
</div>

<%@include file="hiddenStatus.jsp" %>

<div id="mask_div" class="ui_mask" style="width: 1920px; height: 1746px; position: absolute; top: 0px; left: 0px; display: none; background-color: rgb(0, 0, 0); opacity: 0.6; z-index: 999; background-position: initial initial; background-repeat: initial initial;"></div>
<div id="formDiv" style="display:none;" class="pop_layer_v2">
</div>
<script type="text/javascript">
$(function(){
	// AJAX去加载页面, 显示加载图片，加载目标页面，隐藏加载图片
	ajaxALink('#formDiv');
	commonServerPagination('#areaListTable', undefined, "${ctx}/ps/area/arealist.do", {'editorField':['openTime', 'combineTime'], 'searchvalue':function(){
		return $('#ipsearchinput').val();
	}, 'searchtype': function(){
		return $('#search_type_btn').attr('value');
	}});
	$('.searchbutton').click(function(){
		$('#areaListTable').datagrid('reload');
	});
	selectListInit({listChoose: function($lia){
		$lia.closest('.mod_select_down').next().find('textarea').attr('placeholder', $lia.attr('targetPlaceholder'));
	}});
	
	$('#ipsearchinput').bind('keypress',function(event){
		if(event.keyCode == "13"){
			event.preventDefault();
			$('.searchbutton').click();
		}
	});
});

function dateSaveCallback(obj, choice){
	var $idObj = $(obj).closest('td[field=openTime]').parent().find('td[field=worldId]');
	var worldId = $idObj.text();
	if(choice == 1){ //save
		var input = $(obj).parent().find('input').val();
	    if(!input){
	    	alert("时间不能为空");
			return;
	    }
	    var data = $('#areaListTable').datagrid('getData');
	    var rows;
		for(var i = 0; i < data.rows.length; i++){
			if(data.rows[i].worldId == worldId){
				var oldTime = data.rows[i].openTime;
				if(oldTime == input){
					alert("时间没有修改");
					return;
				}else{
					rows = data.rows[i];
					break;
				}
			}
		}
		
	
		$.post('${ctx}/ps/area/modifyopentime.do', {worldId:worldId, openTime:input}, function(data){
				if(data.key == 0){
					$('#areaListTable').datagrid('cancelEdit', i);
					alert('修改失败');
				}else{
					$('#areaListTable').datagrid('endEdit', i);
					rows.openTime = data.value;
					$('#areaListTable').datagrid('reload');
				}
		});	
	}else{
		var data = $('#areaListTable').datagrid('getData');
		for(var i = 0; i < data.rows.length; i++){
			if(data.rows[i].worldId == worldId){
				$('#areaListTable').datagrid('cancelEdit', i);
				break;
			}
		}
		
	}
}

function dateSaveCallback2(obj, choice){
	var $idObj = $(obj).closest('td[field=combineTime]').parent().find('td[field=worldId]');
	var worldId = $idObj.text();
	if(choice == 1){ //save
		var input = $(obj).parent().find('input').val();
	    if(!input){
	    	alert("时间不能为空");
			return;
	    }
	    var data = $('#areaListTable').datagrid('getData');
	    var rows;
		for(var i = 0; i < data.rows.length; i++){
			if(data.rows[i].worldId == worldId){
				var oldTime = data.rows[i].combineTime;
				if(oldTime == input){
					alert("时间没有修改");
					return;
				}else{
					rows = data.rows[i];
					break;
				}
			}
		}
		
		$.post('${ctx}/ps/area/modifycombinetime.do', {worldId:worldId, combineTime:input}, function(data){
				if(data.key == 0){
					$('#areaListTable').datagrid('cancelEdit', i);
					alert('修改失败');
				}else{
					$('#areaListTable').datagrid('endEdit', i);
					rows.combineTime = data.value;
					$('#areaListTable').datagrid('reload');
				}
		});
	}else{
		var data = $('#areaListTable').datagrid('getData');
		for(var i = 0; i < data.rows.length; i++){
			if(data.rows[i].worldId == worldId){
				$('#areaListTable').datagrid('cancelEdit', i);
				break;
			}
		}
		
	}
}

function openTimeFormatter(value,row,index){
	if(value && value.length == 'yyyy-MM-dd hh:mm:ss'.length + 2){
		var ret = value.substr(0, value.length - 2);
		return ret;
	}
	return value;
}

function toShow(){
	  $('#mask_div').show();
	  $('#formDiv').css("position", "absolute");
	  var left = ($('.content_main').width() - $('#formDiv').width()) / 2;
	  $('#formDiv').css("left", left);
	  $('#formDiv').show();
	  $('#formDiv').draggable({handle:'.pop_layer_title'});
 }
 function cancel(){
	  $('#mask_div').hide();
	  $('#formDiv').hide();
 }
 
 function popSubmit(){
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
             $('.content_main').html(data);
         }
     });
 }
 
 function exportExcel(){
		window.open('${ctx}/ps/area/export.do');
	}
</script>
