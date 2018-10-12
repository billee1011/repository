<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<% request.setAttribute("ctx", request.getContextPath()); %>
<link href="${ctx}/css/css.css" rel="stylesheet" media="screen">
<style type="text/css">
<!--
.file{ filter:alpha(opacity:0);opacity: 0;}
-->
</style>
<div class="title_area">
	<h2>公告管理</h2>
</div>
<input id="ctx" type="hidden" value="${ctx }">
<div class="bd" style="padding-top: 10px;margin-bottom: 10px;">
     <div id="content_area" class="table_area" style="overflow: auto; vertical-align: top;">
         <div class="to_redeem_div">
            <div><h3><span id="announce_pop_error" class="tip_error"></span></h3></div>
            <div>
			    <button class="btn btn_blue" onclick="toSelMultiArea(1, '${ctx}')">选区</button>
			    <button class="btn btn_blue" onclick="announcingList()">发布中的公告</button>
			    <button class="btn btn_blue" onclick="announceHistory()">历史公告</button>
			</div>
			<div>
			    <span class="tip">选择公告模板：</span>
			    <select id="select_title" onchange="selectOne()" class="inputlength mail_select_title" >
			    	<option value="-1">无</option>
			    	<c:forEach items="${announceTemplate }" var="entity">
						<option value="${entity.id }">${entity.title }</option>
					</c:forEach>
			    </select>
			    <c:forEach items="${announceTemplate }" var="entity">
					<input id="template${entity.id }" type="hidden" value="${entity.content }"/>
				</c:forEach>
			</div>
			<div>
			<span class="tip" style="vertical-align: top;">公告内容：</span><script id="announceContent" name="announceContent" style="height:200px"></script>
			</div>
			<div>
                <span class="tip" style="margin-left: 2px;">时间间隔：</span>
			    <input id="announceInterval" name="announceInterval" class="intonly" value="0"/>秒
			</div>
			<div>
			   <span class="tip" style="margin-left: 2px;">有效时间：</span>
			   <input id="createAnnounceStart" name="startTime" class="input" maxlength="11" readonly="readonly" onfocus="WdatePicker({skin:'whyGreen',dateFmt:'yyyy-MM-dd HH:mm:ss'})"/>
			   -
			    <input id="createAnnounceEnd" name="endTime" class="input" maxlength="11" readonly="readonly" onfocus="WdatePicker({skin:'whyGreen',dateFmt:'yyyy-MM-dd HH:mm:ss'})"/>
			</div>
			<div>
			    <button class="btn btn_blue" onclick="announceCommit()">发布公告</button>
			    <button class="btn btn_blue" onclick="announceReset()">重置</button>
			    <button class="btn btn_blue" onclick="saveTemplate()">修改当前模板</button>
			    <button class="btn btn_blue" onclick="deleteTemplate()">删除当前模板</button>
			    <button class="btn btn_blue" onclick="addTemplate()">新增</button>
			</div>
         </div>
     </div>
</div>

 <%@include file="../../mutiselectarea.jsp" %> 
 
 <div id="announcingList" style="display:none;" class="pop_layer_v2">
</div>
 <div id="annoucingDeleteDialog" style="display:none;" class="pop_layer_v2">
</div>

<div id="dialogAddTemplate" style="display: none;margin:10px 10px 10px 0px">
	<div style="margin-left: 20px">
		<input type="hidden" id="templateId">
		<p style="margin:10px;">标题:<input type="text" id="template_title"></p>
		<p style="margin:10px;">内容:<textarea id="templateContent" name="announceContent" rows="14" cols="75" style="width: auto;"></textarea></p>
	</div>
</div>

<div id="mask_div" class="ui_mask" style="width: 1920px; height: 1746px; position: absolute; top: 0px; left: 0px; display: none; background-color: rgb(0, 0, 0); opacity: 0.6; z-index: 999; background-position: initial initial; background-repeat: initial initial;"></div>
<script type="text/javascript">
var editor = new baidu.editor.ui.Editor({toolbars:[['source','forecolor','bold']]});  
editor.render( 'announceContent' ); //此处的参数值为编辑器的id值  

function saveTemplate(){
	$("#dialogAddTemplate").show();
	$("#templateId").val($("#select_title").val());
	$("#template_title").val($("#select_title").find("option:selected").text());
	$("#templateContent").val(editor.getContent());
	$("#dialogAddTemplate").dialog({
		title:"修改模板",
		buttons:[{
			text:"提交",
			handler:function(){
				$.ajax({url:"${ctx}/pss/announce/updateTemplate.do",
					data:{
						id:$("#templateId").val(),
						title:$("#template_title").val(),
						content:$("#templateContent").val()
					},
					beforeSend: function()         				// 锁住屏幕
			        {
			            $("body").block({message:"<image src='images/WaitProcess.gif'>",css:{border:'none',backgroundColor:'transparent'},overlayCSS:{opacity:0}});
			        },
					success:function(data){
						$("body").unblock();
						$(".content_main").html(data);
						$("#announce_pop_error").text("修改模板成功");
					}
				});
			}
		}
		]
	});
	$(".content_main").append($("div[class='panel window']"));
	$(".content_main").append($("div[class='window-shadow']"));
}

function deleteTemplate(){
	$.ajax({
		url:"${ctx}/pss/announce/deleteTemplate.do",
		data:{id:$("#select_title").val()},
		success:function(data){
			$(".content_main").html(data);
			$("#announce_pop_error").text("删除模板成功");
		}
	});
}

function selectOne(){
	var now = $("#select_title").val();
	editor.setContent($("#template"+now).val());
}

function addTemplate(){
	$("#dialogAddTemplate").show();
	$("#template_title").val("");
	$("#templateContent").val("");
	$("#dialogAddTemplate").dialog({
		title:"新增模板",
		buttons:[{
			text:"提交",
			handler:function(){
				$.ajax({url:"${ctx}/pss/announce/insertTemplate.do",
					data:{
						title:$("#template_title").val(),
						content:$("#templateContent").val()
					},
					beforeSend: function()         				// 锁住屏幕
			        {
			            $("body").block({message:"<image src='images/WaitProcess.gif'>",css:{border:'none',backgroundColor:'transparent'},overlayCSS:{opacity:0}});
			        },
					success:function(data){
						$("body").unblock();
						$(".content_main").html(data);
						$("#announce_pop_error").text("新增模板成功");
					}
				});
			}
		}
		]
	});
	$(".content_main").append($("div[class='panel window']"));
	$(".content_main").append($("div[class='window-shadow']"));
}

</script>

