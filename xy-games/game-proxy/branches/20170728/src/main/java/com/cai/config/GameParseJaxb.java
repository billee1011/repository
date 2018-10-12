/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.config;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

/**
 *
 * @author wu_hc
 */
public class GameParseJaxb {
	public static void main(String[] args) {

		try {
			JAXBContext context = JAXBContext.newInstance(GameStruct.class);
			Unmarshaller unmarshaller = context.createUnmarshaller();
			GameStruct struct = (GameStruct) unmarshaller.unmarshal(new File("src/main/resources/game.xml"));
			System.out.println(struct);

		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}
}
