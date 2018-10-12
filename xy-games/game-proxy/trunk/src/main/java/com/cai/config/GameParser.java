/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.config;

import java.io.File;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.cai.common.config.struct.HostNode;
import com.cai.common.util.WRSystem;
import com.cai.core.SystemConfig;
import com.google.common.collect.Lists;

/**
 *
 * @author wu_hc
 */
public class GameParser {

	private static GameConfig gameConfig = new GameConfig();

	public static void main(String[] args) {
		parseGameXML();
	}

	public static void parseGame() {
		try {
			JAXBContext context = JAXBContext.newInstance(GameConfig.class);
			Unmarshaller unmarshaller = context.createUnmarshaller();
			// GameConfig struct = (GameConfig) unmarshaller.unmarshal(new
			// File("src/main/resources/game.xml"));
			GameConfig struct = (GameConfig) unmarshaller.unmarshal(new File(WRSystem.HOME + "config/game.xml"));
			System.out.println(struct);

		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 */
	@SuppressWarnings("unchecked")
	public static void parseGameXML() {
		try {
			// File f = new File("global_config/wu/config/game.xml");
			File f = new File(WRSystem.HOME + "config/game.xml");
			SAXReader reader = new SAXReader();
			Document doc = reader.read(f);
			Element root = doc.getRootElement();
			gameConfig.setDebug(Boolean.parseBoolean(root.elementText("debug")));
			gameConfig.setPort(Integer.parseInt(root.elementTextTrim("port")));
			gameConfig.setIndex(Integer.parseInt(root.elementTextTrim("index")));

			List<HostNode> hostNodeList = Lists.newArrayList();
			List<Element> logicSvrsEles = root.element("logicServers").elements("hostNode");
			System.out.println(logicSvrsEles.size());
			for (final Element e : logicSvrsEles) {
				HostNode node = new HostNode();
				node.setIndex(Integer.parseInt(e.attributeValue("index")));
				node.setHost(e.attributeValue("host"));
				node.setStatus(Integer.parseInt(e.attributeValue("status")));
				hostNodeList.add(node);
			}
			gameConfig.setLogicServers(hostNodeList);
			System.out.println(gameConfig);
		} catch (DocumentException e) {
			e.printStackTrace();
		}
	}

	public static void parseGamePeroperties() {
		gameConfig.setDebug(SystemConfig.gameDebug == 1);
		gameConfig.setPort(SystemConfig.game_socket_port);
		gameConfig.setIndex(SystemConfig.proxy_index);

		List<HostNode> hostNodeList = Lists.newArrayList();
		HostNode node = new HostNode();
		node.setIndex(1);
		node.setHost(SystemConfig.loginc_socket_ip + ":" + SystemConfig.logic_sockcet_port);
		node.setStatus(1);
		hostNodeList.add(node);

		gameConfig.setLogicServers(hostNodeList);
	}

	public static GameConfig getGameConfig() {
		return gameConfig;
	}
}
