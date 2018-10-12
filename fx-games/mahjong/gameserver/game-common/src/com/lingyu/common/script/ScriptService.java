package com.lingyu.common.script;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.luaj.vm2.luajc.LuaJC;

import com.google.common.base.Joiner;
import com.lingyu.common.core.ServiceException;

public class ScriptService {
	private static final Logger logger = LogManager.getLogger(ScriptService.class);
	private Globals globals;
	private String rootDir;
	private static ScriptService instance;

	private ScriptService() {
	}

	public static ScriptService getInstance() {
		if (null == instance) {
			instance = new ScriptService();
		}
		return instance;
	}

	/**
	 * 初始化脚本系统
	 * 
	 * @param rootDir
	 *            脚本根目录
	 * @param enableLuaJC
	 *            启用LuaJC
	 * @throws ServiceException
	 */
	public void init(String rootDir, boolean enableLuaJC) throws ServiceException {
		logger.info("init script service: rootDir={}", rootDir);
		logger.info("对应的lua版本为 {}", 5.2);
		this.rootDir = rootDir;
		// create an environment to run in
		globals = JsePlatform.standardGlobals();
		if (enableLuaJC) {
			// 如果不用LUAJC模式lua代码会编译成lua字节码，如果用
			// luaJC模式，则用它会编译成java字节码，这种模式下执行效率会高一些，但是调试定位错误会差一些。
			LuaJC.install(globals);
		}
		// 注册变量
		registerGlobal("rootDir", rootDir);
	}

	public void run(String fileName) throws ServiceException {

		// Use the convenience function on Globals to load a chunk.
		String file = Joiner.on(File.separator).join(rootDir, fileName);
		// Use any of the "call()" or "invoke()" functions directly on the
		// chunk.
		globals.loadfile(file).call();
	}

	public void registerGlobal(String name, Object obj) throws ServiceException {

		try {
			globals.set(name, CoerceJavaToLua.coerce(obj));
		} catch (LuaError e) {
			throw new ServiceException(e);
		}
	}

	public Globals getGlobal() {
		return globals;
	}

}
