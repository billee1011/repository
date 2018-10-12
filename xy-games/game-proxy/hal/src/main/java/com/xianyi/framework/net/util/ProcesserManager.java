package com.xianyi.framework.net.util;

import java.io.File;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.math.NumberUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.EMsgType;
import com.cai.common.util.WRSystem;
import com.cai.net.core.ClientHandler;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.ExtensionRegistry;

import javolution.util.FastMap;
import protobuf.clazz.Protocol;

public final class ProcesserManager
{
	private static final Logger logger = LoggerFactory.getLogger(ProcesserManager.class);
	
	private static FastMap<Integer, RequestClassHandlerBinding<ClientHandler>> mapping = new FastMap<Integer, RequestClassHandlerBinding<ClientHandler>>(300);

	private static FastMap<Class, Integer> responseMapping = new FastMap<Class, Integer>(300);
	
	private static Map<Integer,Byte> resPriority =new FastMap<Integer,Byte>();
	
	static 
	{
		loadRequestHandler("../common/request-handler.xml");
		loadResponseMapping("../common/response-mapping.xml");
	}
	
	public static void reloadRequestHandlerMapping()
	{
		loadRequestHandler("../common/request-handler.xml");
	}
	
	public static void reloadResponseMapping()
	{
		loadResponseMapping("../common/response-mapping.xml");
	}
	
	private static void loadRequestHandler(String path)
	{
		try
		{
			String conf = WRSystem.HOME + path;
			SAXReader reader = new SAXReader();
			Document document = reader.read(new File(conf));
			Element root = document.getRootElement();
			Iterator<Element> it = root.elementIterator("request");
			
			
			ExtensionRegistry registry = ExtensionRegistry.newInstance();
			Protocol.registerAllExtensions(registry);
			
			synchronized (mapping)
			{
				while (it.hasNext())
				{
					Element e = it.next();
					int requestType = NumberUtils.createNumber(e.attributeValue("id")).intValue();
					RequestClassHandlerBinding requestHandlerBinding = new RequestClassHandlerBinding();
					
					
					int msgType = NumberUtils.createNumber(e.attributeValue("msgType")).intValue();
					EMsgType eMsgType = EMsgType.getEMsgType(msgType);
					if(eMsgType!=EMsgType.LOGIC_MSG){
						FieldDescriptor fieldDescriptor = registry.findExtensionByName(e.attributeValue("extensionName")).descriptor;
						if(fieldDescriptor==null){
							logger.error("request-handler.xml,找不到扩展"+e.attributeValue("extensionName"));
						}
						requestHandlerBinding.setFieldDescriptor(fieldDescriptor);//扩展
					}
					requestHandlerBinding.setHandlerClass(Class.forName(e.attributeValue("handlerClass")));
					
					if(eMsgType!=null){
						requestHandlerBinding.seteMsgType(eMsgType);
					}else{
						logger.error("找不到消息类型,EMsgType="+eMsgType.getId());
					}
					
					if(mapping.containsKey(requestType))
					{
						if(!(mapping.get(requestType).toString().equals(requestHandlerBinding.toString())))
						{
							StringBuilder handerBuf = new StringBuilder();
							handerBuf.append("Handler updated (").append(requestType).append(") from ").append(mapping.get(requestType).toString())
							.append(" to ").append(requestHandlerBinding.toString());
							System.out.println(handerBuf.toString());
							
							
							mapping.put(requestType, requestHandlerBinding);
						}
					}
					else
					{
						mapping.put(requestType, requestHandlerBinding);
						StringBuilder buf = new StringBuilder();
						buf.append("New handler registerd {").append(requestType).append(requestHandlerBinding.toString()).append("}");
						System.out.println(buf.toString());
					}
				}
				System.out.println("注册Handlers完成");
			}
		}
		catch (Exception e1)
		{
			e1.printStackTrace();
		}
	}
	
	private static void loadResponseMapping(String path)
	{
		try
		{
			String conf = WRSystem.HOME + path;
			SAXReader reader = new SAXReader();
			Document document = reader.read(new File(conf));
			Element root = document.getRootElement();
			Iterator<Element> it = root.elementIterator("response");
			while (it.hasNext())
			{
				Element e = it.next();
				int responseType = NumberUtils.createNumber(e.attributeValue("id")).intValue();
				responseMapping.put(Class.forName(e.attributeValue("responseClass")), responseType);
				if(e.attributeValue("priority")!=null)
				{
					resPriority.put(responseType,Byte.parseByte(e.attributeValue("priority")));
				}
			}
		}
		catch (Exception e1)
		{
			e1.printStackTrace();
		}
	}
	
	private ProcesserManager()
	{

	}
	
	public static int getResponseType(Class responseClass)
	{
		return responseMapping.get(responseClass);
	}

	public static void registResponseType(Class responseClass, int responseType)
	{
		responseMapping.put(responseClass, responseType);
	}

	public static RequestClassHandlerBinding<ClientHandler> getRequestClassHandlerBinding(int requestType)
	{
		return mapping.get(requestType);
	}

	

	public static byte getResPriority(int responseType)
	{
		if(resPriority.get(responseType)!=null)
		{
			return resPriority.get(responseType);
		}
		return 0;
	}
	
}
