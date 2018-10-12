<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<style type="text/css">
<!--
#formDiv{
 width:400px;
 }
-->
</style>
<div class="pop_layer_main">
<div class="pop_layer_title">
   <h3>平台管理<span id="pop_error"  class="tip_error"></span></h3>
</div>
<div class="pop_layer_cont">
            <div class="tip">确认删除${platform.name }吗?</div>
             <form:form method="POST" modelAttribute="platform" id="postform">
                <form:input path="id" type="hidden"/>
                <div class="pop_layer_ft"><a class="btn btn_submit not_close btn_blue" href="javascript:popSubmit();"><span class="">确认</span></a><a href="javascript:cancel();" class="btn btn_white_2 btn_close"><span class="">取消</span></a></div>
            </form:form>
</div>
</div>
<script type="text/javascript">
$(function(){
	toShow();
});
  </script>
