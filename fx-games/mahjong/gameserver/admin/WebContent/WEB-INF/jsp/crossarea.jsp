<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%request.setAttribute("ctx", request.getContextPath());%>
<div class="pop_layer_main">
<div class="pop_layer_title">
   <h3>选服管理
   <span id="pop_error" class="tip_error"></span>
   </h3>
</div>
<div class="pop_layer_cont">
<div class="selectarea">
   <c:forEach items="${crossAreas }" var="area" varStatus="areaStatus">
       <a href="javascript:;" onclick="javascript:chooseArea(this, ${area.worldId }, 'hiddenSelectedAreaId');" class="selectspan onlyselectspan used_${area.used?1:0}" title="<spring:message code="server.status.${area.status}"></spring:message>">
              <c:choose>
                  <c:when test="${area.status == 0}"> 
		              <img alt="" src="${pageContext.request.contextPath}/images/home/leader_run.png"/>
                  </c:when>
                  <c:when test="${area.status == 1}"> 
		              <img alt="" src="${pageContext.request.contextPath}/images/home/leader_maintain.png"/>
                  </c:when>
                  <c:otherwise>
		              <img alt="" src="${pageContext.request.contextPath}/images/home/leader_stop.png"/>
                  </c:otherwise>
              </c:choose>
       ${area.worldName }</a><c:if test="${(areaStatus.index + 1) % 10 == 0 }"><br/></c:if>
   </c:forEach>
</div>
<div class="pagearea">
    <c:if test="${curPage > 1 }">
       <a href="javascript:toSelectCross(1);">首页</a>
        <a href="javascript:toSelectCross(${curPage - 1 });">上一页</a>
    </c:if>
    <c:forEach items="${displayPages }" var="displayPage">
        <c:choose>
           <c:when test="${displayPage == curPage }">${displayPage }</c:when>
           <c:otherwise><a href="javascript:toSelectCross(${displayPage });">${displayPage }</a></c:otherwise>
        </c:choose>
    </c:forEach>
    <c:if test="${curPage < totalPage }">
        <a href="javascript:toSelectCross(${curPage + 1 });">下一页</a>
        <a href="javascript:toSelectCross(${totalPage });">尾页</a>
    </c:if>
</div>
<input type="hidden" id="hiddenSelectedAreaId" value="-1"/>
<div class="pop_layer_ft"><a class="btn btn_submit not_close btn_blue" href="javascript:selectCrossArea($('#hiddenSelectedAreaId').val(), 'pop_error', '${callback }');"><span class="">选择</span></a><a class="btn btn_white_2 btn_close" href="javascript:cancelPop('crossformDiv');"><span>取消</span></a></div>
</div>
</div>
<script type="text/javascript">
    function toSelectCross(curPage){
    	toSelectCrossArea(curPage, '${ctx }', '${crossType }', '${callback}');
    }
</script>