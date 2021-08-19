package com.project.concurrent.myAQS;

import com.tools.Tools;

import java.util.HashMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

public class Test {
    static volatile int a = 0;


    public static void main(String[] args) {
//        extracted();
        ConcurrentHashMap<Integer, String> map = new ConcurrentHashMap<>();
        map.put(1,"一");
        System.out.println(map.size());
        map.put(1,"二");
        System.out.println(map.size());
    }


    private static void extracted() {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        for (int i = 0; i < 8; i++) {
            pool.execute(() -> {
                int val = a;
//                System.out.println(Thread.currentThread().getName()+":"+val);
                for (int j = 0; j < 10; j++) {
                    val++;
                }
//                System.out.println(Thread.currentThread().getName()+":"+val);
                a += val;
                System.out.println(val);
            });
        }
        pool.shutdown();
        Tools.sleep(100);
        System.out.println(a);
    }
}
