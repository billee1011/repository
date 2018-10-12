<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%request.setAttribute("ctx", request.getContextPath());%>
<style type="text/css">
<!--
#formDiv{
 width:400px;
 }
-->
</style>
<div class="pop_layer_main">
<div class="pop_layer_title">
   <h3>公告管理<span id="pop_error" class="tip_error"></span></h3>
</div>
<div class="pop_layer_cont">
            
            <div class="tip">确认删除${announce.id }吗?</div>
             <form:form method="POST" modelAttribute="announce" id="deleteAnnounceform">
                <form:input path="id" type="hidden" id="deleteAnnounceId"/>
                <div class="pop_layer_ft"><a class="btn btn_submit not_close btn_blue" href="javascript:confirmDeleteAnnounce();"><span class="">确认</span></a><a href="javascript:cancelPop('annoucingDeleteDialog');" class="btn btn_white_2 btn_close"><span class="">取消</span></a></div>
            </form:form>
</div>
</div>
<script type="text/javascript">
$(function(){
	toShowAnnoucingDeleteDialog();
});

function confirmDeleteAnnounce(){
	var url = $('#deleteAnnounceform').attr('action');
	var id = $('#deleteAnnounceId').val();
	deleteAnnounce(url, {'id':id});
	cancelPop('annoucingDeleteDialog');
}
  </script>
