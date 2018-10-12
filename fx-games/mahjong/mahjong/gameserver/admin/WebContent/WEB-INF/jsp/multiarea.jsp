<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%request.setAttribute("ctx", request.getContextPath());%>
<div class="selectarea">
   <c:forEach items="${gameAreas }" var="area" varStatus="areaStatus">
       <a href="javascript:;" class="<c:if test="${lastarea.areaId eq area.areaId }">defaultSelectArea </c:if>selectspan onlyselectspan multiSelectArea withId_${area.areaId}" areaId="${area.areaId }" worldId="${area.worldId }" follower="${area.followerAreaId }" onclick="multiChooseArea(this, '${area.areaId}', 'ghostStore', 'mulSelectAreaDiv', null, '${area.worldId }');" title="<spring:message code="server.status.${area.status}"></spring:message>">
          <c:choose>
            <c:when test="${area.followerAreaId == 0 }">
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
            </c:otherwise></c:choose>${area.areaName }
       </a>
       <c:if test="${(areaStatus.index + 1) % 10 == 0 }"><br/></c:if>
   </c:forEach>
</div>
<div class="pagearea">
    <c:if test="${curPage > 1 }">
       <a href="javascript:toSelMultiArea(1, '${ctx }');">首页</a>
        <a href="javascript:toSelMultiArea(${curPage - 1 }, '${ctx }');">上一页</a>
    </c:if>
    <c:forEach items="${displayPages }" var="displayPage">
        <c:choose>
           <c:when test="${displayPage == curPage }">${displayPage }</c:when>
           <c:otherwise><a href="javascript:toSelMultiArea(${displayPage }, '${ctx }');">${displayPage }</a></c:otherwise>
        </c:choose>
    </c:forEach>
    <c:if test="${curPage < totalPage }">
        <a href="javascript:toSelMultiArea(${curPage + 1 }, '${ctx }');">下一页</a>
        <a href="javascript:toSelMultiArea(${totalPage }, '${ctx }');">尾页</a>
    </c:if>
</div>
<script type="text/javascript">
    $(function(){
    	var $dialog = $('#mulSelectAreaDiv');
    	var $virginSet = $('.virginSet:first');
   	    if($virginSet.size() > 0 && $virginSet.val().length == 0){
   	    	$virginSet.val('1');
   	    	$dialog.find('.defaultSelectArea').click();
   	    }
   	    autoSelectSelectedAreas('ghostStore');
    });
</script>