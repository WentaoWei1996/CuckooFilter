package com.wwt.main;

import com.wwt.cuckoofilter.CuckooFilter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @Author: wwt
 * @Date: 2020/11/27 22:55
 */
public class Main {

    public static void main(String[] args) {

        testCuckooFilter();
//        System.out.println();
//        testCuckooFilterOfGithub();
        while (true) {

        }
    }

    public static void testCuckooFilter() {
        CuckooFilter cuckooFilter = new CuckooFilter(1 << 22);
        Map<String, Double> map = read();

        Set<String> keySet = map.keySet();
        for (String key : keySet) {
            cuckooFilter.insert(key, map.get(key));
        }

        int num = map.size();
        int equal = 0;
        for (String key : keySet) {
            double value = cuckooFilter.get(key);
            if (value == map.get(key)) {
                equal++;
            }
        }

        System.out.println("get Value Rate");
        System.out.println(num - equal);
        System.out.println(equal * 1.0 / num);
        System.out.println();

        equal = 0;
        for (String key : keySet) {
            if (cuckooFilter.contains(key)) {
                equal++;
            }
        }
        System.out.println("get Exist Rate");
        System.out.println(num - equal);
        System.out.println(equal * 1.0 / num);
    }

    public static void testCuckooFilterOfGithub() {
        com.github.cuckoofilter.CuckooFilter cuckooFilter = new com.github.cuckoofilter.CuckooFilter(1 << 20);

        Map<String, Double> map = read();

        Set<String> keySet = map.keySet();
        for (String key : keySet) {
            cuckooFilter.insert(key, map.get(key));
        }

        int num = map.size();

        int equal = 0;
        for (String key : keySet) {
            double value = cuckooFilter.get(key);
            if (value == map.get(key)) {
                equal++;
            }
        }

        System.out.println("get Value Rate");
        System.out.println(num - equal);
        System.out.println(equal * 1.0 / num);
        System.out.println();

        equal = 0;
        for (String key : keySet) {
            if (cuckooFilter.contains(key)) {
                equal++;
            }
        }
        System.out.println("get Exist Rate");
        System.out.println(num - equal);
        System.out.println(equal * 1.0 / num);
    }

    public static Map<String, Double> read() {

        Map<String, Double> res = new HashMap<>();

        File file = new File("/Users/weiwentao/key_value.txt");
        List<String> lines = new ArrayList<>();

        try {
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);

            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }

            br.close();
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (String line : lines) {
            String[] splits = line.split("\t");
            res.put(splits[0], Double.parseDouble(splits[1]));
        }

        return res;
    }
}
