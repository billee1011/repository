package com.cai.game.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

class FileCompare {
    private static final Map<Integer, Boolean> map1 = new HashMap<>();
    private static final Map<Integer, Boolean> map2 = new HashMap<>();

    private static void compare(String file1, String file2, String file3) {
        load_1(file1);
        load_2(file2);

        PrintWriter stdout = null;
        try {
            stdout = new PrintWriter(file3);

            stdout.println("map1's size: " + map1.size() + " map2's size: " + map2.size());

            Iterator<Entry<Integer, Boolean>> it = map1.entrySet().iterator();

            while (it.hasNext()) {
                Entry<Integer, Boolean> entry = it.next();
                int key = entry.getKey();

                if (!map2.containsKey(key)) {
                    stdout.println(key);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            stdout.close();
        }
    }

    private static void load_1(String file1) {
        if ("".equals(file1) || null == file1) {
            // System.err.println("文件名不能为空！");
            return;
        }

        Scanner stdin = null;
        try {
            stdin = new Scanner(new File(file1));

            while (stdin.hasNextInt()) {
                map1.put(stdin.nextInt(), true);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            stdin.close();
        }
    }

    private static void load_2(String file2) {
        if ("".equals(file2) || null == file2) {
            // System.err.println("文件名不能为空！");
            return;
        }

        Scanner stdin = null;
        try {
            stdin = new Scanner(new File(file2));

            while (stdin.hasNextInt()) {
                map2.put(stdin.nextInt(), true);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            stdin.close();
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < GameUtilConstants.TABLE_COUNT; i++) {
            String file1 = "tbl/feng_eye_table_" + i + ".tbl";
            String file2 = "tbl_backup/feng_eye_table_" + i + ".tbl";
            String file3 = "tbl/compare_feng_eye_table_" + i + ".tbl";

            compare(file1, file2, file3);
        }
    }
}
