package com.pt.test;

import com.cai.common.constant.Symbol;
import com.google.common.primitives.Longs;

/**
 * @author wu_hc date: 2018年08月12日 下午3:00:19 <br/>
 */
public final class ExciteTest {

	public static void main(String[] args) {

//		ExciteConditionGroup group = new ExciteConditionGroup();
//
//		CoinExciteModel model = new CoinExciteModel();
//		model.setId(1);
//		model.setCategory(ECardCategory.SURPLUS.getCategory());
//		model.setVar1(1);
//		model.setVar2(3);
//		model.setOutput(10);
//		model.setTriggerType(ETriggerType.OVER.getCategory());
//		group.addCondition(model);
//
//		model = new CoinExciteModel();
//		model.setId(2);
//		model.setCategory(ECardCategory.SURPLUS.getCategory());
//		model.setVar1(4);
//		model.setVar2(5);
//		model.setOutput(20);
//		model.setTriggerType(ETriggerType.OVER.getCategory());
//		group.addCondition(model);
//
//		model = new CoinExciteModel();
//		model.setId(3);
//		model.setCategory(ECardCategory.SURPLUS.getCategory());
//		model.setVar1(6);
//		model.setVar2(9);
//		model.setOutput(30);
//		model.setTriggerType(ETriggerType.OVER.getCategory());
//		group.addCondition(model);
//
//		group.setCallBack((id, output) -> System.out.println("达成id:" + id + " 倍数:" + output));
//
//		group.triggerEvent(ETriggerType.OVER, ECardCategory.SURPLUS, 7);
//
//		System.out.println("over all output:" + group.getOverOutput());

		//0x00000040
		System.out.println(String.format("0x%08x",64));

		String ct = "11:0x00000040";
		String[] arr = ct.split(Symbol.COLON);
		System.out.println(String.format("%s:%d",arr[0],Long.parseLong(arr[1].substring(2))));
	}
}
