<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%
	request.setAttribute("ctx", request.getContextPath());
%>
<style>
table {
	border-collapse: separate;
	border-spacing: 10px 5px;
}
</style>
<div class="title_area">
	<h2>版本公告</h2>
</div>
<div style="margin-left:20px">
<table>
	<tr>
		<td>类型</td>
		<td>
			<select id="select_noticeType" >
		    	<option value="1">版本公告</option>
		    	<option value="2">官方公告</option>
			</select>
			版本号：<input type="text" name="version" class="input" value="" id="noticeversion"/>
		</td>
	</tr>
	<tr>
		<td>更新公告</td><td><script id="content"  style="width:100%;height:500px;" ></script></td>
	</tr>
	<tr>
		<td><input type="button" name="submit" class="btn btn_blue" value="发布" id="btn_submit"/></td><td></td>
	</tr>
</table>
</div>

<textarea id="updateingAnnounceContent" style="display:none;"></textarea>

<script>
var editor = new baidu.editor.ui.Editor();  
editor.render( 'content' ); //此处的参数值为编辑器的id值  
editor.addListener( 'ready', function( e ) {
	editor.setContent($('#updateingAnnounceContent').val());
} );
$(function(){
	$('#btn_submit').bind('click',function(){
		var type = $('#select_noticeType').val();
		var version = $('#noticeversion').val();
		var content = editor.getContent();
		$.ajax({url:"${ctx}/pss/announce/updateNotice.do",data:{type:type,content:content, version: version},method:'post',
			success:function(data){
				alert('更新公告成功');
			}
		});
	});
});
</script>
