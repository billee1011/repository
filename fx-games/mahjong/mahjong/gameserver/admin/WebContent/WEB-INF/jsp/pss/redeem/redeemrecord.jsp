<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%request.setAttribute("ctx", request.getContextPath());%>
<input type="hidden" id="hiddenCheckRight" value="${checkRight }"/>
<div class="table_area" style="overflow: auto;">
    <div class="table_mask" id="cont_table_area" style="width: 100%;">
         <c:choose>
		    <c:when test="${recordsCount le 0}">
			  <div class="table_blank">
				<p class="text">还没有补偿记录</p>
		      </div>
			</c:when>
			<c:otherwise>
			   <div>
					<table id="redeemRecordList" style="height: 600px;">
					  <thead style="height: 31px">
						<tr>
							<th data-options="field:'id',width:calColumnWidth(0.04)">ID</th>
						    <th data-options="field:'addTime',width:calColumnWidth(0.09),formatter:function(value,row,index){
								var date = new Date(value);
								return date.bfFormatter();
							}">时间</th>
							<th data-options="field:'adminName',width:calColumnWidth(0.06)">管理员</th>
							<th data-options="field:'ip',width:calColumnWidth(0.06)">IP</th>
							<th data-options="field:'displayareas',width:calColumnWidth(0.1),formatter:function(value,row,index){
								if(row['allArea']){
								    return '所有服务器';
								}
							    return row['areas'];
							}">区服</th>
							<th data-options="field:'allArea',hidden:true">是否全服玩家</th>
							<th data-options="field:'areas',hidden:true">所选区服</th>
							<th data-options="field:'isAll',hidden:true">是否所有玩家</th>
							<th data-options="field:'players',width:calColumnWidth(0.1),formatter:function(value,row,index){
							     if(row.all){
							         return '全部玩家';
							     }else{
							     	return value;
							     }
							}">目标玩家</th>
							<th data-options="field:'coin',width:calColumnWidth(0.03)">铜币</th>
							<th data-options="field:'diamond',width:calColumnWidth(0.03)">元宝</th>
							<th data-options="field:'items',width:calColumnWidth(0.35)">道具</th>
							<th data-options="field:'status',hidden:true">状态</th>
							<th data-options="field:'statusValue',width:calColumnWidth(0.04),formatter:function(value,row,index){
							    if(row.status == 0){
							        return '未审核';
							    }else if(row.status == 1){
							    	 return '审核通过';
							    }else if(row.status == 2){
							    	 return '审核未通过';
							    }
							}">状态</th>
							<th data-options="field:'check',width:calColumnWidth(0.05),formatter:function(value,row,index){
							    if(row['status'] == 0){
							    	var checkRight = $('#hiddenCheckRight').val();
							    	if('true' == checkRight){
							    	    return '<a href=javascript:; onclick=checkRedeem('+row.id+',true)>通过</a> | <a href=javascript:; onclick=checkRedeem('+row.id+',false)>拒绝</a>';
							    	}
							    }
							}">审核</th>
						</tr>
					  </thead>
					  <tbody>
					  </tbody>
					</table>
			    </div>
			</c:otherwise>
		</c:choose>
    </div>
</div>
<script type="text/javascript">
<!--
   $(function(){
	   commonServerPagination('#redeemRecordList', undefined, "${ctx}/pss/redeem/redeemrecord.do");
   });
   
   function checkRedeem(id, accepted){
	   $.post('${ctx}/pss/redeem/checkredeem.do', {id:id, accepted:accepted}, function(data){
		    var msg = "失败";
			if(data == null || data.retCode == 1){
				msg = "成功";
				$('#redeemRecordList').datagrid('reload');
			}else if(data.retCode == 1001){  //异步操作  以日志记录为准
				msg = "成功: 以日志记录为准";
				$('#redeemRecordList').datagrid('reload');
			}
			$('#redeemResultMsg').html(msg);
			$(".redeemResultError").html("");
			if(data.messages != null && data.messages.length > 0){
				for(var i = 0; i < data.messages.length; i++){
					$(".redeemResultError").append(data.messages[i] + '<br/>');
				}
			}
			popUp('redeemResult');
	   });	   
   }
//-->
</script>
