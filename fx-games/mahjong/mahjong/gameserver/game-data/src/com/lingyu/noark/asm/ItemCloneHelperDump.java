package com.lingyu.noark.asm;

import java.io.File;
import java.io.FileOutputStream;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ItemCloneHelperDump implements Opcodes {

	private static FileOutputStream out;

	public static void main(String[] args) throws Exception {
		out = new FileOutputStream(new File("a.class"));
		out.write(dump());
		out.flush();
	}

	public static byte[] dump() throws Exception {

		ClassWriter cw = new ClassWriter(0);
		FieldVisitor fv;
		MethodVisitor mv;

		cw.visit(V1_7, ACC_PUBLIC + ACC_SUPER, "com/lingyu/noark/asm/ItemCloneHelper", null, "java/lang/Object", null);

		cw.visitSource("ItemCloneHelper.java", null);

		{
			fv = cw.visitField(0, "h", "Lcom/lingyu/noark/asm/AttributeCloneHelper;", null, null);
			fv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
			Label l1 = new Label();
			mv.visitLabel(l1);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitTypeInsn(NEW, "com/lingyu/noark/asm/AttributeCloneHelper");
			mv.visitInsn(DUP);
			mv.visitMethodInsn(INVOKESPECIAL, "com/lingyu/noark/asm/AttributeCloneHelper", "<init>", "()V");
			mv.visitFieldInsn(PUTFIELD, "com/lingyu/noark/asm/ItemCloneHelper", "h", "Lcom/lingyu/noark/asm/AttributeCloneHelper;");
			Label l2 = new Label();
			mv.visitLabel(l2);
			mv.visitInsn(RETURN);
			Label l3 = new Label();
			mv.visitLabel(l3);
			mv.visitMaxs(3, 1);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC, "clone", "(Lcom/lingyu/noark/data/entity/Item;)Lcom/lingyu/noark/data/entity/Item;", null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);

			// 说明：在初始化一般对象时，我们需要先调用NEW指令，来创建该对象实例。而由于
			// 后续的INVOKESPECIAL指令是调用类的构造函数，而该指令执行完以后，对对象的引
			// 用将从栈中弹出，所以在NEW指令执行完以后，INVOKESPECIAL指令执行以前，我们
			// 需要调用DUP指令，来增加对象引用的副本。
			mv.visitTypeInsn(NEW, "com/lingyu/noark/data/entity/Item");
			mv.visitInsn(DUP);
			mv.visitMethodInsn(INVOKESPECIAL, "com/lingyu/noark/data/entity/Item", "<init>", "()V");
			mv.visitVarInsn(ASTORE, 2);// 将引用存到局部变量栈2号的位置
			Label l1 = new Label();
			mv.visitLabel(l1);
			mv.visitVarInsn(ALOAD, 2);
			mv.visitVarInsn(ALOAD, 1);// 1号是参数source
			mv.visitMethodInsn(INVOKEVIRTUAL, "com/lingyu/noark/data/entity/Item", "getId", "()J");
			mv.visitMethodInsn(INVOKEVIRTUAL, "com/lingyu/noark/data/entity/Item", "setId", "(J)V");
			Label l2 = new Label();
			mv.visitLabel(l2);
			mv.visitVarInsn(ALOAD, 2);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKEVIRTUAL, "com/lingyu/noark/data/entity/Item", "getName", "()Ljava/lang/String;");
			mv.visitMethodInsn(INVOKEVIRTUAL, "com/lingyu/noark/data/entity/Item", "setName", "(Ljava/lang/String;)V");
			Label l3 = new Label();
			mv.visitLabel(l3);
			mv.visitVarInsn(ALOAD, 2);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKEVIRTUAL, "com/lingyu/noark/data/entity/Item", "isBind", "()Z");
			mv.visitMethodInsn(INVOKEVIRTUAL, "com/lingyu/noark/data/entity/Item", "setBind", "(Z)V");
			Label l4 = new Label();
			mv.visitLabel(l4);
			mv.visitVarInsn(ALOAD, 2);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, "com/lingyu/noark/asm/ItemCloneHelper", "h", "Lcom/lingyu/noark/asm/AttributeCloneHelper;");
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKEVIRTUAL, "com/lingyu/noark/data/entity/Item", "getAttribute", "()Lcom/lingyu/noark/data/entity/Attribute;");
			mv.visitMethodInsn(INVOKEVIRTUAL, "com/lingyu/noark/asm/AttributeCloneHelper", "clone",
					"(Lcom/lingyu/noark/data/entity/Attribute;)Lcom/lingyu/noark/data/entity/Attribute;");
			mv.visitMethodInsn(INVOKEVIRTUAL, "com/lingyu/noark/data/entity/Item", "setAttribute", "(Lcom/lingyu/noark/data/entity/Attribute;)V");
			Label l5 = new Label();
			mv.visitLabel(l5);
			mv.visitVarInsn(ALOAD, 2);
			mv.visitInsn(ARETURN);
			Label l6 = new Label();
			mv.visitLabel(l6);
			mv.visitMaxs(3, 3);
			mv.visitEnd();
		}
		cw.visitEnd();

		return cw.toByteArray();
	}
}
