<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<link href="${pageContext.request.contextPath}/css/css.css" rel="stylesheet" media="screen">
<div class="pop_layer_main">
<div class="pop_layer_title">
   <h3>游戏服管理
   <span id="pop_error" class="tip_error"></span>
   </h3>
</div>
<div class="pop_layer_cont">
<form:form method="POST" modelAttribute="area" id="postform">
<table class="ui_formTable"> 
<!-- 请指定各个列宽 -->
<colgroup>
    <col style="width:80px"> 
    <col style="width:auto">
    </colgroup><tbody>
    <tr> 
       <th scope="row">ID:</th>
       <td>
       <form:input path="areaId" class="input" maxlength="11"/>                      
        </td>      
    </tr>
    <tr> 
       <th scope="row">区名:</th>
       <td>
       <form:input path="areaName" class="input" maxlength="11"/>                      
        </td>       
    </tr>
    <tr>
        <th scope="row">类型：</th>
        <td>
         <form:input path="type" class="input" maxlength="11"/>
       </td> 
    </tr>
    <tr>
        <th scope="row">IP：</th>
        <td>
         <form:input path="ip" class="input" maxlength="20"/>
       </td> 
    </tr>
    <tr>
        <th scope="row">端口：</th>
        <td>
         <form:input path="port" class="input" maxlength="11"/>
       </td> 
    </tr>
    <tr>
        <th scope="row">平台：</th>
        <td>
         <form:input path="pid" class="input" maxlength="11"/>
       </td> 
    </tr>
    <tr>
        <th scope="row">状态：</th>
        <td>
                                   是 <form:radiobutton path="valid" value="true" style="border:0px;"/>
        否    <form:radiobutton path="valid" value="false" style="border:0px;"/>
       </td> 
    </tr>
    <tr>
        <th scope="row">创建时间：</th>
        <td>
         <form:input path="addTime" class="input" maxlength="11" readonly="readonly" onfocus="WdatePicker({skin:'whyGreen',dateFmt:'yyyy-MM-dd HH:mm:ss'})"/>
       </td> 
    </tr>
    <tr>
        <th scope="row">修改时间：</th>
        <td>
         <form:input path="modifyTime" class="input" maxlength="11" readonly="readonly" onfocus="WdatePicker({skin:'whyGreen',dateFmt:'yyyy-MM-dd HH:mm:ss'})"/>
       </td> 
    </tr>
    </tbody>
    
 </table>
    <div class="pop_layer_ft"><a class="btn btn_submit not_close btn_blue" href="javascript:popSubmit();"><span class="">更新</span></a><a class="btn btn_white_2 btn_close" href="javascript:cancel();"><span>取消</span></a></div>
</form:form>
</div>
</div>
<script type="text/javascript">
$(function(){
	toShow();
});
</script>
