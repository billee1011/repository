package com.cai.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.Timer;
import java.util.TimerTask;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.SimpleScriptContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.domain.Event;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.WRSystem;
import com.cai.core.MonitorEvent;
import com.cai.domain.Session;

import javolution.util.FastMap;


/**
 * java脚本管理器
 * @author run
 * @date 2014-11-10
 */
public class JavaScriptServiceImpl  extends AbstractService{
	
	private static final Logger log = LoggerFactory.getLogger(JavaScriptServiceImpl.class);

	public static  File SCRIPT_FOLDER = new File(WRSystem.HOME+"../common/", "data/scripts");
	
	private ScriptEngineManager scriptEngineManager;
	
	private Map<String,String> scriptNameMap;
	
	//需要检测的脚本
	private Map<String,Long> reqCheckScriptMap;
	
	//常量
	public static final String CALCULATE = "calculate";
	public static final String ITEM = "item";
	
	
	
	/**
	 * 单例
	 */
	private static JavaScriptServiceImpl instance;
	/**
	 * 获取单例
	 * 
	 * @return;
	 */
	public static JavaScriptServiceImpl getInstance()
	{
		if (instance == null){
			instance = new JavaScriptServiceImpl();
		}
		return instance;
	}
	
	private JavaScriptServiceImpl(){
		PerformanceTimer timer = new PerformanceTimer();
		scriptEngineManager = new ScriptEngineManager();
		List<ScriptEngineFactory> factories = scriptEngineManager.getEngineFactories();
		StringBuilder buf = new StringBuilder();
		buf.append("当前JVM可用的脚本引擎:");
		for (ScriptEngineFactory factory : factories)
		{
			buf.append("\n")
			.append("EngineName:"+factory.getEngineName() + ",version:"+factory.getEngineVersion());
			ScriptEngine engine = factory.getScriptEngine();
		}
		log.info(buf.toString());
		ScriptEngine javaEngine = scriptEngineManager.getEngineByName("java");//直接获取java引擎
		if(javaEngine==null){
			log.error("初始化java引擎失败");
		}
		
		//脚本名对应的关联文件
		scriptNameMap = new FastMap<String, String>();		
		scriptNameMap.put(CALCULATE, "data/CalculateExpression.cfg");
		scriptNameMap.put(ITEM, "data/Item.cfg");
		
		
		log.info("初始化java脚本管理器"+timer.getStr());
		
	}
	
	/**
	 * 加载java脚本
	 * @param file
	 * @return
	 */
	public boolean loadJavaScript(File file){
		ScriptEngine javaEngine = scriptEngineManager.getEngineByName("java");
		
		PerformanceTimer timer = new PerformanceTimer();
		//上下文备份
		ScriptContext ctx = javaEngine.getContext();
		//构建新的上下文
		ScriptContext context = new SimpleScriptContext();
		try {
			
			if(!file.exists()){
				log.error("找不到脚本:"+file.getPath());
				return false;
			}
			StringBuilder buf = new StringBuilder();
			buf.append("加载脚本:"+file.getPath());
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			context.setAttribute("mainClass", getClassForFile(file).replace('/', '.').replace('\\', '.'),ScriptContext.ENGINE_SCOPE);
			context.setAttribute(ScriptEngine.FILENAME, file.getName(), ScriptContext.ENGINE_SCOPE);
			context.setAttribute("classpath", SCRIPT_FOLDER.getAbsolutePath(), ScriptContext.ENGINE_SCOPE);
			context.setAttribute("sourcepath", SCRIPT_FOLDER.getAbsolutePath(), ScriptContext.ENGINE_SCOPE);
			//使用新的上下文来编译
			javaEngine.setContext(context);
			Compilable eng = (Compilable) javaEngine; 
			timer.reset();
			CompiledScript cs = eng.compile(reader);
			buf.append(",动态编译:"+timer.getStr());
			timer.reset();
			cs.eval(context);
			buf.append(",动态执行main"+timer.getStr());
			log.info(buf.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			javaEngine.setContext(ctx);
			context.removeAttribute(ScriptEngine.FILENAME, ScriptContext.ENGINE_SCOPE);
			context.removeAttribute("mainClass", ScriptContext.ENGINE_SCOPE);
			
		}
				
		
		return true;
	}
	
	public static String getClassForFile(File script)
	{
		String path = script.getAbsolutePath();
		String scpPath = SCRIPT_FOLDER.getAbsolutePath();
		if (path.startsWith(scpPath))
		{
			int idx = path.lastIndexOf('.');
			return path.substring(scpPath.length() + 1, idx);
		}
		return null;
	}
	
	
	/**
	 * 加脚本(启动时)
	 * @param scriptName
	 */
	public void initLoad(String...arguments){
		reqCheckScriptMap =  new FastMap<String, Long>();
		for(int i = 0;i < arguments.length;i++)
		{
			String scriptName = arguments[i];
			
			//加载对应的脚本文件
			String path = WRSystem.HOME+"../common/" + scriptNameMap.get(scriptName);
			
			LineNumberReader lnr = null;
			try {
				File file = new File(path);
				if(!file.isFile())
					continue;
				 lnr = new LineNumberReader(new InputStreamReader(new FileInputStream(path)));
				String line;
				while ((line = lnr.readLine()) != null &&  !"".equals(line.trim()))
				{
					//是否是注释行
					if(line.indexOf("#")==-1){
						//找到真正的脚本文件位置
						File scriptFile = new File(JavaScriptServiceImpl.SCRIPT_FOLDER, line.trim());
						loadJavaScript(scriptFile);
						reqCheckScriptMap.put(scriptName,file.lastModified());
					}
				}
				
			} catch (Exception e) {
				log.error("error",e);
			}finally{
				try {
					if(lnr!=null)
						lnr.close();
				} catch (IOException e) {
					log.error("error",e);
				}
			}
		}
		
		//定时器
		Timer timer = new Timer("Game Script check Timer");
		timer.schedule(new TimerTask(){
			@Override
			public void run() {
				check();
			}
		}, 3000, 3000);
	}
	
	/**
	 * 定时检测是否重载脚本
	 */
	public void check(){
		
		for (String scriptName : reqCheckScriptMap.keySet()) {
			//加载对应的脚本文件
			String path = WRSystem.HOME+"../common/" + scriptNameMap.get(scriptName);
			File file = new File(path);
			//是否有修改
			if(file.lastModified()==reqCheckScriptMap.get(scriptName))
				continue;
			
			LineNumberReader lnr = null;
			try {
				if(!file.isFile())
					continue;
				 lnr = new LineNumberReader(new InputStreamReader(new FileInputStream(path)));
				String line;
				while ((line = lnr.readLine()) != null &&  !"".equals(line.trim()))
				{
					//是否是注释行
					if(line.indexOf("#")==-1){
						//找到真正的脚本文件位置
						File scriptFile = new File(JavaScriptServiceImpl.SCRIPT_FOLDER, line.trim());
						loadJavaScript(scriptFile);
						reqCheckScriptMap.put(scriptName,file.lastModified());
					}
				}
			} catch (Exception e) {
				log.error("error",e);
			}finally{
				try {
					lnr.close();
				} catch (IOException e) {
					log.error("error",e);
				}
			}
			
		}
	}

	@Override
	protected void startService() {
		JavaScriptServiceImpl.getInstance().initLoad(JavaScriptServiceImpl.CALCULATE);
	}

	@Override
	public MonitorEvent montior() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onEvent(Event<SortedMap<String, String>> event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sessionCreate(Session session) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sessionFree(Session session) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dbUpdate(int _userID) {
		// TODO Auto-generated method stub
		
	}
	
	

	
	
	
}
