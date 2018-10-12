<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<% request.setAttribute("ctx", request.getContextPath()); %>
<div class="title_area" style="height: 85px">
	<h2>游戏区在线</h2>
	<form>
		<div class="r relative" id="datePicker">
			<a class="time" id="datePicker_a">
			<span><p>日期选择<b id="displayDateTip" style="font-weight:normal"> : 查询日期</b><br><font class="calendar" id="dateValue">2014-06-04</font></p></span></a>
			<div class="timetxt hide panone" id="datePanel" style="width: 278px; left: 0px; display: none;">
				<div id="dateBanner" class="timetop"> 
				</div>
				<p class="TimeDef" id="timeInputRoom">
					自定<input type="text" id="startTime" name="startTime" class="Timeinput">到 <input type="text" id="endTime" name="endTime" class="Timeinput mrnone">
				</p>
				<div class="opeDiv after">
					<small id="datamessage" style="display: none; margin-left: 30px;" class="l"></small>
					<a class="timecolse r Confirm" id="confirmBtn">确定</a>
					<a class="r Cancel" id="cancelBtn">取消</a>
				</div>
			</div>
		</div>
	</form>
</div>
<div class="condition_area" id="condition_area_div">
               <div class="mod_select_down">
                   <a href="javascript:void(0);" onclick="return false;" id="search_type_btn" class="btn_white_2 btn_select_outline" value="${areaId}"><span><c:forEach items="${gameAreas }" var="gameArea"><c:if test="${areaId == gameArea.areaId}">${gameArea.areaName }</c:if></c:forEach></span><i class="ico"></i></a>
                   <div id="search_type_list_div">
                   <ul class="down_list btn_select_outline" id="search_type_list" style="position:static;border:none">
                   		 <c:forEach items="${gameAreas }" var="gameArea">
                   			<li><a href="javascript:void(0);" onclick="return false;" _search_type="1" value="${gameArea.areaId }">${gameArea.areaName }</a></li>
                   		</c:forEach>
					</ul>
					</div>
               </div>
               <div class="mod_search ">
                   <button type="submit" id="btn_search"><span class="visually_hidden">查询</span></button>
               </div>
</div>
<div class="bd" style="padding-top: 10px;">
	<div id="tabs" class="easyui-tabs" style="height: auto;">
		<div title="区">
			<div class="easyui-tabs childTabs" style="height: auto; margin : 10px">
				<div title="图表">
					<div id="container0" style="height:400px; margin: 20px 10px 20px 10px"></div>
				</div>
				<div title="数据视图">
					<div>
						<table id="dataGrid0" style="padding-top: 10px;height: 600px;overflow: scroll;"></table>
					</div>
				</div>
			</div>
		</div>
		<div title="平台">
			<div class="easyui-tabs childTabs" style="height: auto; margin : 10px">
				<div title="图表">
					<div id="container1" style="height:400px; margin: 20px 10px 20px 10px"></div>
				</div>
				<div title="数据视图">
					<div>
						<table id="dataGrid1" style="padding-top: 10px;height: 600px;overflow: scroll;"></table>
					</div>
				</div>
			</div>
		</div>
	</div>
	
</div>
<script type="text/javascript">

var detailChart = [];
var masterChart = [];
var statisticsData = [];
var nowIndex = 0;
var maxSize = 0;//适应Y轴title长度
var queryConfig;//请求配置
var areaUrl = "${ctx}/stat/online/online4area.do";

function createDetail(masterChart,datas,index) {
    // prepare the detail chart
    /**
    var detailData = datas,
        detailStart = Date.UTC(1000, 7, 1);
    for(var a = 0 ; a < masterChart.series[0].length ; a++){
	    $.each(masterChart.series[a].data, function () {
	    	if (this.x >= detailStart) {
	            detailData[a].push(this.y);
	        }
	    });
    }*/
    // create a detail chart referenced by a global variable
    detailChart = $('#detail-container'+index).highcharts({
        chart: {
        	type:'area',
            reflow: false,
            marginLeft: maxSize * 10 + 20,
            marginRight: 20,
            style: {
                position: 'absolute'
            }
        },
        credits: {
            enabled: false
        },
        title: {
            text: '在线人数统计图'
        },
        tooltip:{
			formatter:function(){
							return this.series.name+" "+Highcharts.dateFormat("%H:%M:%S",this.x)+"<br/>人数:"+this.y+"人";
						}
		},
        xAxis: {
            type: 'datetime',
            labels:{
            	formatter:function(){
            		return Highcharts.dateFormat('%H:%M',this.value);
            	}
            }
        },
        yAxis: {
            title: {
                text: null
            },
            floor:0,
            maxZoom: 0.1
        },
        legend: {
           	align: 'right',
            verticalAlign: 'top',
            x:-70
        },
        plotOptions: {
            series: {
                marker: {
                    enabled: false
                },
                states: {
                    hover: {
                        enabled: false
                    }
                }
            },
            area: {
                fillColor: {
                    linearGradient: { x1: 0, y1: 2, x2: 0, y2: 0},
                    stops: [
                        [0, Highcharts.getOptions().colors[0]],
                        [1, Highcharts.Color(Highcharts.getOptions().colors[0]).setOpacity(0).get('rgba')]
                    ]
                },
                lineWidth: 1,
                marker: {
                    enabled: false
                },
                shadow: false,
                states: {
                    hover: {
                        lineWidth: 1
                    }
                },
                threshold: null
            }
        },
        series: datas

    }).highcharts(); // return chart
}

// create the master chart
function createMaster(datas,index) {
	masterChart = $('#master-container'+index).highcharts({
        chart: {
        	type:'spline',
            reflow: false,
            borderWidth: 0,
            backgroundColor: null,
            marginLeft: maxSize * 10 + 20,
            marginRight: 20,
            zoomType: 'x',
            events: {
                selection: function (event) {
                	/***
                    var extremesObject = event.xAxis[0],
                        min = extremesObject.min,
                        max = extremesObject.max,
                        detailData = [],
                        xAxis = this.xAxis[0];
	                    xAxis.removePlotBand('mask-after');
	                    xAxis.addPlotBand({
	                        id: 'mask-after',
	                        from: max,
	                        to: min,
	                        color: 'rgba(0, 0, 0, 0.2)'
	                    });
	                    for(var a = 0; a < this.series.length; a++){
		                    detailDataChild = [];
	                    	$.each(this.series[a].data, function () {
		                        if (this.x > min && this.x < max) {
		                            detailDataChild.push([this.x,this.y]);
		                        }
		                    });
	                    	detailChart.series[a].setData(detailDataChild);
	                    }***/
	                    return false;
	                }
            }
        },
        title: {
            text: null
        },
        xAxis: {
            type: 'datetime',
            labels:{
            	formatter:function(){
            		return Highcharts.dateFormat('%H:%M',this.value);
            	}
            },
            title: {
                text: null
            }
        },
        yAxis: {
            labels: {
                enabled: false
            },
            floor:0,
            title: {
                text: null
            }
        },
        tooltip: {
            formatter: function () {
                return false;
            }
        },
        legend: {
            enabled: false
        },
        credits: {
            enabled: false
        },
        plotOptions: {
            series: {
                fillColor: {
                    linearGradient: [0, 0, 0, 70],
                    stops: [
                        [0, Highcharts.getOptions().colors[0]],
                        [1, 'rgba(255,255,255,0)']
                    ]
                },
                lineWidth: 1,
                marker: {
                    enabled: false
                },
                shadow: false,
                states: {
                    hover: {
                        lineWidth: 1
                    }
                },
                enableMouseTracking: false
            }
        },
        series: datas

    }, function (masterChart) {
        createDetail(masterChart,datas,index);
    })
        .highcharts(); // return chart instance
}

function updateOnlineNum(isCharts){
	var exportUrl;
	if(nowIndex == 0){
		showDiv(true);
		ChartsGetData(areaUrl,nowIndex);
		exportUrl = "${ctx}/stat/online/export.do";
	}else if(nowIndex == 1){
		showDiv(false);
		ChartsGetData("${ctx}/stat/online/online4platform.do",nowIndex);
		exportUrl = "${ctx}/stat/online/exportPlat.do";
	}
	var data = statisticsData[nowIndex];
	if(data.length == 0 ){
		return;
	}
	data[0].color = '#8abced';
	data[0].lineWidth=1;
	if(data[1]){
		data[1].color = '#10296b';
		data[1].lineWidth=1;
		
		
	}
	if(isCharts){
		/***
		var result = data[data.length-1];
		var resultValue = result.data;
		var last = resultValue[resultValue.length-1];
		var startTimeHour = getHMS(last[0]);
		var endTimeHour = getQueryEndTime(last[0]);
		var startTime = result.name+" "+startTimeHour;
		var endTime = result.name+" "+endTimeHour;
		//只有小时和分钟比之前大才请求
		if(isQueryData(last[0])){
			if(!queryConfig){
				//初始化
				queryConfig = {url:'${ctx}/stat/online/timerOnlineNum.do',data:{startTime:startTime,endTime:endTime}};
			}
			asyncUpdateChart(queryConfig,dynamicUpdateData);
		}*/
		//createMaster(data,nowIndex);
		createDetail(null,data,nowIndex);
	}else{
		createDataGridView(data,nowIndex,exportUrl);
	}
}



function initChart(){
	$("#tabs").tabs({
		border:true,
		tabWidth:200,
		tabHeight:40,
		onSelect:function(title){
			if($("#tabs .easyui-tabs:visible>.tabs-panels").size() == 0){
				$("#tabs .easyui-tabs:visible").each(function(){
					$(this).tabs({
						border:true,
						onSelect:function(title){
							var parentTitleList = $("#tabs>.tabs-header>").find(".tabs-title");
							var nowParentTitle = $(".tabs>.tabs-selected>.tabs-inner>.tabs-title").html();
							for(var a = 0 ; a < parentTitleList.length ; a++){
								var info = parentTitleList.get(a).innerHTML;
									if(info == nowParentTitle){
										nowIndex = a;
										updateOnlineNum(title == "图表");
										break;
									}
							}
						}
					})
				})
			}else{
				var nowChildTitle = $("tabs>.tabs-panels");
				var list = $(".tabs-title");
				for(var a = 0 ; a < list.length ; a++){
					var info = list.get(a).innerHTML;
						if(info == title){
							var nowChildTitle = $(".childTabs:visible .tabs-selected .tabs-title").html();
							nowIndex = a;
							updateOnlineNum(nowChildTitle == "图表");
							break;
						}
				}
			}
		}
		});
}

function refreshInfo(){
	var childTitle = $(".childTabs:visible .tabs-selected .tabs-title").html();
	updateOnlineNum(childTitle == "图表");
}

$(function(){
	registerBfSelect();
	for(var i = 0 ; i < 3 ; i++){
		statisticsData[i] = [];
		var $container = $('#container'+i).css('position', 'relative');
		$('<div id="detail-container'+i+'">').appendTo($container);
		$('<div id="master-container'+i+'">').css({ position: 'absolute', top: 300, height: 100, width: '100%' })
		    .appendTo($container);
	}
	initDatePicker({func:refreshInfo,days:1});
	initChart();
	selectServerList();
});

function createDataGridView(data,index,exportUrl){
	var list = new Array();
	for(var a = 0 ; a < data.length ; a++){
		for(var b = 0 ; b < data[a].data.length ; b++){
			list.push({num:data[a].data[b][1],time:data[a].name+" "+Highcharts.dateFormat("%H:%M:%S",data[a].data[b][0])});
		}
	}
	list.sort(function(z,x){
		var zdate = new Date(z.time.replace(/-/g,"/"));
		var xdate = new Date(x.time.replace(/-/g,"/"));
		return zdate.getTime()-xdate.getTime()>0?1:-1;
	});
	var beginTime = $("#startTime").val();
	var endTime = timeStrAdd($("#endTime").val(),1);
	$("#dataGrid"+index).datagrid({
		data:list,
		fitColumns:true,
		rowStyler:function(index,row){
			return 'height:50px';
		},
		columns:[[
					{field:"time",title:"时间",align:"center",width:25},
					{field:"num",title:"日活跃玩家数",align:"center",width:25},
					//{title:"<a href='"+exportUrl+"?beginTime="+beginTime+"&endTime="+endTime+"'>导出excel</a>",align:"center",width:25}
					{title:"<a href='#' onclick='exportExcel()'>导出excel</a>",align:"center",width:25}
		        ]]
	});
}

function ChartsGetData(url,index){
	statisticsData[index] = [];
	$.ajax({url:url,data:{"startTime":$("#startTime").val(),"endTime":timeStrAdd($("#endTime").val(),1),areaId:getQueryAreaId()},async:false,
		success:function(data) {
			if(!data[0]){
				return;
			}
			var beforeTime = new Date(getRealServerTime(data[0].addTime,'${timezonerawoffset}')).Format("yyyy-MM-dd");
			var result = {data:[]};
			 for(var i = 0 ; i < data.length ; i++){
				 var date = new Date(getRealServerTime(data[i].addTime,'${timezonerawoffset}'));		//时区转换
				 //var date = new Date(data[i].addTime);
				 if(date.Format("yyyy-MM-dd") != beforeTime){
					 result.name = beforeTime;
					 statisticsData[index].push(result);
					 beforeTime = date.Format("yyyy-MM-dd");
					 result = {data:[]};
				 }
					 var hour = date.getHours();
					 var min = date.getMinutes();
					 var sec = date.getSeconds();
				 maxSize = Math.max(data[i].num.toString().length,maxSize);
				 //按照小时和分钟来比较两组数据需要确保年 月 日都是一样的
				 result.data.push([Date.UTC(2000,1,1,hour,min,sec),data[i].num]);
			 }
			result.name = beforeTime;
			statisticsData[index].push(result);
		}
	});
}
function dynamicUpdateData(data){
	if(data.length>0){
		var series = detailChart.series;
		var updateIndex = 0;
	    if(series != undefined){
	    	updateIndex = series.length-1;
	    	var localZone = new Date().getTimezoneOffset() * 60000;
	    	var chartData = detailChart.series[updateIndex].data;
	    	var minX= 0;
			if(chartData.length>0){
				var curValue = chartData[chartData.length-1];
				minX = curValue.x;
			}
	    	for(var i =0;i<data.length;i++){
				var toAddDate = new Date(parseInt(data[i].addTime) - localZone);		//时区转换
				toAddDate.setUTCFullYear(2000, 1, 1);
				var x = toAddDate.getTime();
				var y = data[i].num;
				if(x>minX){
					detailChart.series[updateIndex].addPoint([x,y],true,false);
					masterChart.series[updateIndex].addPoint([x,y],true,false);
					//需要更新请求条件时间
					var startTime = series[updateIndex].name+" "+ getHMS(x);
					var endTime = series[updateIndex].name+" "+ getQueryEndTime(x);
					if(isQueryData(x)){
						queryConfig.data={startTime:startTime,endTime:endTime};
					}
					else{
						//需要停止定时器
						stopTimer(queryConfig.timer);
						//console.log("定时器停止运行");
					}
				}
				else{
					//console.log("不符合时间："+x+" 值："+minX);
				}
	    	}
	    }
	}
}


//根据url 异步获取数据  回调更新图表
function asyncUpdateChart(config,updateChart){
	//如果定时器存在  则关闭之前的定时器
	if(config.timer!=undefined){
		clearInterval(config.timer);
	}
	config.timer = setInterval(function(){
		$.ajax({url:config.url,data:config.data,async:config.async||false,
			success:function(data){
				updateChart(data);
			}});
	},config.time||1000*60*5);
}

function getQueryEndTime(startTime){
	//大概是10分钟的间隔
	return getHMS(startTime + 600000);
}

//获取格式化后的结果
function getHMS(time){
	
   return Highcharts.dateFormat("%H:%M:%S",time);
}

//比较时间
function isQueryData(startTime){
	var startTimeHour = getHMS(startTime);
	var endTimeHour = getQueryEndTime(startTime);
	var startArray = startTimeHour.split(":");
	var endArray = endTimeHour.split(":");
	if(endArray[0]>startArray[0] || ((endArray[0] == startArray[0]) && (endArray[1]>startArray[1]))){
		return true;
	}
	return false;
}

//停止定时器
function stopTimer(timer){
	if(timer!=undefined){
		clearInterval(timer);
	}
}

//导出Excel
//服务器参照：https://raw.githubusercontent.com/highslide-software/highcharts.com/master/studies/csv-export/csv.php
function exportExcel(){
	var data = $("#dataGrid"+nowIndex).datagrid('getData');
	var headArray = [{time:"时间",num:"人数"}];
	postCSVData("在线人数.xls",headArray.concat(data.rows));
}

/**
 *filename 文件名
 *data     数据 array
 ***/
function postCSVData(filename,data){
	if(!data){
		return;
	}
	var line ='';
	for(var i =0;i<data.length;i++){
		var num = data[i].num;
		var time = data[i].time;
		if(line == ''){
			line = time+","+num;
		}
		else{
			line += ";" + time+","+num;
		}
	}
	//window.open('${ctx}/stat/exportfile/file.do?data='+line+'&type=csv&fileName='+filename);
	Highcharts.post('${ctx}/stat/exportfile/file.do',{
	        data: line,
	        type: 'csv',
	        fileName:filename
	      });
}

</script>
