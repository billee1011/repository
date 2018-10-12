 ================================多区选择======================================
  1） <%@include file="mutiselectarea.jsp" %>   目录为源jsp相对于该jsp的路径
  2) 源jsp中添加按钮  click事件为： toSelMultiArea(1, '${ctx}')       param0: 表示第一页   param1:应用上下文
  3) ajax： 提交时js获取选择参数
       //填充选服信息
       //原来要传的参数
		var params = {'mailTitle':mailTitle,'mailContent':mailContent,'selectRoleType':selectRoleType,'roleArray':roleArray.join(','),'money':money,'diamond':diamond,'itemArray':itemArray.join(',')};
		multiAreasFillParams(params);  //该方法会填充选区信息
		全服： name: all  value: true|false
		列表： name: areaList  value: 手选列表blabla
	 或者使用：
	    <input/> 标签实现   	
	              class: multiResultAllArea   选区确定之后为1 表示全服   0:表示手选服务器
	              class: multiResultAreas     手选列表  全服为1的时候 这块是没值的
	手选服务器列表格式： id1,id2,id3,id4,....
       
            多区操作完是不会自动清掉上一次选择的服务器的记录的   multiAreaReset()可以手动调用这个方法清掉
  4)    if(!isMultiSelValid()){
			$('#mail_pop_error').html("没有选定一个区");
			return;
		}
               在submit之前调用这个判断 防止他人不选区操作      
  5) 后端那边就走多服处理的逻辑了  blabla
  eg:  pss/redeem/index.jsp    com.lingyu.admin.controller.pss.RedeemController
  
  
  PS:  Controller端 在处理非全部区的时候需要过滤下（不信任客户端发的数据，需要修正从区的才需要这个）
       if(!all){
          areaList = gameAreaManager.filterGameAreaIds(SessionUtil.getCurrentUser().getLastPid(), areaList);
	   }
   会把选择的从区干掉   服务器自己添加主区中对应的从区
  
  -------------------------原来多区选择蛋疼啊 蛋疼---------------------------------------
  
  
  