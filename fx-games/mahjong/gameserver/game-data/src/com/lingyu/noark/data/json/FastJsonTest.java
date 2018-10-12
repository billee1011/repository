package com.lingyu.noark.data.json;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.Date;

import org.junit.Test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.lingyu.noark.data.EntityMapping;
import com.lingyu.noark.data.accessor.AnnotationEntityMaker;
import com.lingyu.noark.data.entity.Attribute;
import com.lingyu.noark.data.entity.BossFB;
import com.lingyu.noark.data.entity.FBInfo;
import com.lingyu.noark.data.entity.Item;

@SuppressWarnings("unused")
public class FastJsonTest {
	@Test
	public void testxxx() {
		Object xx = new int[] { 11, 2 };
		String text = JSON.toJSONString(xx, SerializerFeature.WriteClassName,SerializerFeature.BeanToArray);
		System.out.println(text);

		Object x = JSON.parseObject(text, Object.class);
		System.out.println(x);
	}

	@Test
	public void testx() {
		BossFB fb = new BossFB();
		fb.getInfos().add(new FBInfo("1"));
		fb.getInfos().add(new FBInfo("2"));
		fb.getInfos().add(new FBInfo("3"));
		System.out.println(JSON.toJSONString(fb, SerializerFeature.WriteClassName));

		System.out.println(JSON.toJSONString(JSON.parseObject(JSON.toJSONString(fb, SerializerFeature.WriteClassName), BossFB.class).getInfos()));
	}

	Item item = new Item();

	{
		item.setId(1234567890);
		item.setName("XXX");
		item.setTemplateId(10010);
		item.setRoleId(9876543210L);
		item.setBind(true);
		item.setAddTime(new Date());
		item.setAttribute(new Attribute(1));
	}

	@Test
	public void test1111() throws CloneNotSupportedException, InstantiationException, IllegalAccessException {
		EntityMapping<Item> em = new AnnotationEntityMaker().make(Item.class);
		for (int j = 0; j < 1000; j++) {
			long start = System.nanoTime();
			for (int i = 0; i < 1000; i++) {
				// item.clone();
				// em.copyx(item);
			}
			System.out.println((System.nanoTime() - start) / 1000000f);
		}

	}

	static int count = 0;

	@SuppressWarnings("rawtypes")
	public static void recursion(Class clazz) {
		for (int i = 0; i < clazz.getDeclaredFields().length; i++) {
			Field currlField = clazz.getDeclaredFields()[i];
			currlField.setAccessible(true);
			String fieldOpt = currlField.getName().toString();
			if (!currlField.getType().isPrimitive()) { // 判断是否是原始类型
				fieldOpt += "  不是原始类型    ";
				try {
					if (currlField.getType().getPackage().getSpecificationTitle().equalsIgnoreCase("Java Platform API Specification") == false) { // 判断是否是Java
																																					// API的类型
						fieldOpt += "  不是Java自带类型   ";
						recursion(currlField.getType());

					} else {
						fieldOpt += "  是Java自带类型   ";
					}
				} catch (NullPointerException e) {
					fieldOpt += " 不是Java自带类型  ";
					recursion(currlField.getType());

				}
			} else {
				fieldOpt += " 是原始类型  ";
			}
			System.out.println(fieldOpt);
		}
	}

	@Test
	public void testJson() {
		Item itemx = JSON.parseObject(JSON.toJSONString(item), Item.class);
		System.out.println(JSON.toJSONString(item));
		try {
			Class.forName("com.lingyu.noark.data.entity.Item");
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		long start = System.nanoTime();

		for (int i = 0; i < 100000; i++) {
			try {
				Item ite = (Item) JSON.parseObject(JSON.toJSONString(item), Class.forName("com.lingyu.noark.data.entity.Item"));
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}

		System.out.println((System.nanoTime() - start) / 1000000f);
	}

	@Test
	public void testJave1() throws Exception {
		long start = System.nanoTime();

		for (int i = 0; i < 100000; i++) {
			Item ix = new Item();
			ix.setId(item.getId());
			ix.setName(item.getName());
			ix.setTemplateId(item.getTemplateId());
			ix.setRoleId(item.getRoleId());
			ix.setBind(item.isBind());
			ix.setAddTime(item.getAddTime());
			ix.setAttribute(new Attribute(1));
		}

		System.out.println((System.nanoTime() - start) / 1000000f);

	}

	@Test
	public void testJave() throws Exception {
		long start = System.nanoTime();

		for (int i = 0; i < 100000; i++) {
			Item ix = new Item();
			ix.setId(item.getId());
			ix.setName(item.getName());
			ix.setTemplateId(item.getTemplateId());
			ix.setRoleId(item.getRoleId());
			ix.setBind(item.isBind());
			ix.setAddTime(item.getAddTime());
			ix.setAttribute(new Attribute(1));
		}

		System.out.println((System.nanoTime() - start) / 1000000f);

	}

	@Test
	public void testJaveByte() throws Exception {

		// return ois.readObject();

		long start = System.nanoTime();

		for (int i = 0; i < 100000; i++) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();

			ObjectOutputStream oos = new ObjectOutputStream(bos);

			// oos.writeObject(this);
			oos.writeObject(item); // 将流序列化成对象
			ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());

			ObjectInputStream ois = new ObjectInputStream(bis);

			Item itemx = (Item) ois.readObject();
		}

		System.out.println((System.nanoTime() - start) / 1000000f);
	}
}
