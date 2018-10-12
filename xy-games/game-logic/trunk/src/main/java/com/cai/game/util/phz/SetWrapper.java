package com.cai.game.util.phz;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

public class SetWrapper {
	private Set<String> set = new HashSet<String>();

	protected boolean contains(String key) {
		return set.contains(key);
	}

	protected void add(String key) {
		set.add(key);
	}

	protected void dump(String filename) {
		if ("".equals(filename) || null == filename) {
			// System.err.println("文件名不能为空！");
			return;
		}

		PrintWriter stdout = null;
		try {
			stdout = new PrintWriter(filename);

			System.out.println(filename + " " + set.size());

			Iterator<String> it = set.iterator();

			while (it.hasNext()) {
				String key = it.next();
				stdout.println(key);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			stdout.close();
		}
	}

	protected void load(String filename) {
		if ("".equals(filename) || null == filename) {
			// System.err.println("文件名不能为空！");
			return;
		}

		Scanner stdin = null;
		try {
			stdin = new Scanner(new File(filename));
			
			while (stdin.hasNextLine()) {
				set.add(stdin.nextLine());
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			stdin.close();
		}
	}
}
