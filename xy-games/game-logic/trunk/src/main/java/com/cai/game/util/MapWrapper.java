package com.cai.game.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

class MapWrapper {
    private Map<Integer, Boolean> map = new HashMap<>();

    protected boolean contains(int key) {
        return map.containsKey(key);
    }

    protected void put(int key) {
        map.put(key, true);
    }

    protected void dump(String filename) {
        if ("".equals(filename) || null == filename) {
            // System.err.println("文件名不能为空！");
            return;
        }

        PrintWriter stdout = null;
        try {
            stdout = new PrintWriter(filename);

            System.out.println(filename + " " + map.size());

            Iterator<Entry<Integer, Boolean>> it = map.entrySet().iterator();

            while (it.hasNext()) {
                Entry<Integer, Boolean> entry = it.next();
                stdout.println(entry.getKey());
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

            while (stdin.hasNextInt()) {
                map.put(stdin.nextInt(), true);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            stdin.close();
        }
    }
}
