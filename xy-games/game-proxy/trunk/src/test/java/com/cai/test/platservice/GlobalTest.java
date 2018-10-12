package com.cai.test.platservice;

import java.util.concurrent.TimeUnit;

import com.cai.core.Global;

public final class GlobalTest {

	public static void main(String[] args) {
		new Thread(() -> {
			for (;;) {
				try {
					TimeUnit.MILLISECONDS.sleep(3000L);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				Global.getWxPayService().execute(() -> {
					System.out.println(Thread.currentThread().getName());
					try {
						Integer.parseInt("xxxxxxxxx");	
						System.out.println("22222222222222222222");
					} catch (Exception e2) {
						e2.printStackTrace();
					}
					

				});
			}
		}).start();
	}
}
