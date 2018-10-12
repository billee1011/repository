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
<div class="displayarea selectspan" title="<spring:message code="server.status.${lastarea.status}"></spring:message>">当前选服：
  <c:choose>
     <c:when test="${lastarea.followerId == 0 }">
              <c:choose>
                  <c:when test="${lastarea.status == 0}"> 
		              <img alt="" src="${pageContext.request.contextPath}/images/home/leader_run.png"/>
                  </c:when>
                  <c:when test="${lastarea.status == 1}"> 
		              <img alt="" src="${pageContext.request.contextPath}/images/home/leader_maintain.png"/>
                  </c:when>
                  <c:otherwise>
		              <img alt="" src="${pageContext.request.contextPath}/images/home/leader_stop.png"/>
                  </c:otherwise>
              </c:choose>
            </c:when>
            <c:otherwise>
              <c:choose>
                  <c:when test="${lastarea.status == 0}"> 
		              <img alt="" src="${pageContext.request.contextPath}/images/home/follower_run.png"/>
                  </c:when>
                  <c:when test="${lastarea.status == 1}"> 
		              <img alt="" src="${pageContext.request.contextPath}/images/home/follower_maintain.png"/>
                  </c:when>
                  <c:otherwise>
		              <img alt="" src="${pageContext.request.contextPath}/images/home/follower_stop.png"/>
                  </c:otherwise>
               </c:choose>
            </c:otherwise>
  </c:choose>  
  ${lastarea.worldId }-${lastarea.areaName }</div>
<div class="selectarea">
   <c:forEach items="${gameAreas }" var="area" varStatus="areaStatus">
       <a href="javascript:;" onclick="javascript:chooseArea(this, ${area.areaId }, 'hiddenSelectedAreaId');" class="selectspan onlyselectspan" title="<spring:message code="server.status.${area.status}"></spring:message>">
          <c:choose>
            <c:when test="${area.followerId == 0 }">
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
            </c:when>
            <c:otherwise>
              <c:choose>
                  <c:when test="${area.status == 0}"> 
		              <img alt="" src="${pageContext.request.contextPath}/images/home/follower_run.png"/>
                  </c:when>
                  <c:when test="${area.status == 1}"> 
		              <img alt="" src="${pageContext.request.contextPath}/images/home/follower_maintain.png"/>
                  </c:when>
                  <c:otherwise>
		              <img alt="" src="${pageContext.request.contextPath}/images/home/follower_stop.png"/>
                  </c:otherwise>
               </c:choose>
            </c:otherwise></c:choose> 
       ${area.areaName }</a><c:if test="${(areaStatus.index + 1) % 10 == 0 }"><br/></c:if>
   </c:forEach>
</div>
<div class="pagearea">
    <c:if test="${curPage > 1 }">
       <a href="javascript:toSelectArea(1, '${ctx }');">首页</a>
        <a href="javascript:toSelectArea(${curPage - 1 }, '${ctx }');">上一页</a>
    </c:if>
    <c:forEach items="${displayPages }" var="displayPage">
        <c:choose>
           <c:when test="${displayPage == curPage }">${displayPage }</c:when>
           <c:otherwise><a href="javascript:toSelectArea(${displayPage }, '${ctx }');">${displayPage }</a></c:otherwise>
        </c:choose>
    </c:forEach>
    <c:if test="${curPage < totalPage }">
        <a href="javascript:toSelectArea(${curPage + 1 }, '${ctx }');">下一页</a>
        <a href="javascript:toSelectArea(${totalPage }, '${ctx }');">尾页</a>
    </c:if>
</div>
<input type="hidden" id="hiddenSelectedAreaId" value="-1"/>
<div class="pop_layer_ft"><a class="btn btn_submit not_close btn_blue" href="javascript:selectArea($('#hiddenSelectedAreaId').val(), 'pop_error', '${ctx }');"><span class="">选择</span></a><a class="btn btn_white_2 btn_close" href="javascript:hideSelectArea();"><span>取消</span></a></div>
</div>
</div>