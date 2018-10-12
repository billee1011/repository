<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<span>
<input type="hidden" id="errorcodeflag"/>
<spring:message code="error.code.${code }" text="${code }"></spring:message>
</span>