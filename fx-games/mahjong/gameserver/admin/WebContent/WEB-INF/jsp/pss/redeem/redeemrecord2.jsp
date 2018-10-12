<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%request.setAttribute("ctx", request.getContextPath());%>
<div>
    <c:forEach items="${redeemRecordList }" var="redeemRecord">
      <div class="redeem_entry">
        <fmt:formatDate value="${redeemRecord.addTime }" pattern="yyyy-MM-dd HH:mm:ss"/>
        <fmt:message key="detail.redeem">
		   <fmt:param value="${redeemRecord.adminName }"></fmt:param>
		   <c:choose>
		       <c:when test="${redeemRecord.allArea }">
		           <fmt:param><spring:message code="detail.redeem.allArea" text="全区"></spring:message></fmt:param>
		       </c:when>
		       <c:otherwise>
		           <fmt:param value="${redeemRecord.areas }"></fmt:param>
		       </c:otherwise>
		   </c:choose>
		  <c:choose>
		       <c:when test="${redeemRecord.all }">
		           <fmt:param><spring:message code="detail.redeem.allPlayers" text="所有玩家"></spring:message></fmt:param>
		       </c:when>
		       <c:otherwise>
		           <fmt:param value="${redeemRecord.players }"></fmt:param>
		       </c:otherwise>
		   </c:choose> 
		   <fmt:param value="${redeemRecord.coin }"></fmt:param>
		   <fmt:param value="${redeemRecord.diamond }"></fmt:param>
		   <fmt:param value="${redeemRecord.items }"></fmt:param>
		</fmt:message>
	  </div>
    </c:forEach>
</div>
