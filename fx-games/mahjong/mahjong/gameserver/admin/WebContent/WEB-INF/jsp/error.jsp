<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<html>
    <head>
        <title>页面错误</title>
    </head>
    <body>
    <%
            out.println("对不起,暂时没有找到您所访问的页面地址,请联系管理员解决此问题.<br/><br/>");
     
    %>
    <a href="javascript:void(0);" onclick="history.go(-1)">返回刚才页面</a><br/><br/>
    </body>
</html>