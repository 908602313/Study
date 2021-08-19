package com.project.concurrent.myAtomicInteger;

import com.project.concurrent.other.MyCountDownLatch;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Test {
    public static void main(String[] args) {
        test1();
    }

    private static void test1() {
        int num = 5;
        MyAtomicInteger atomicInteger = new MyAtomicInteger();
        MyCountDownLatch countDownLatch = new MyCountDownLatch(num);
        ExecutorService pool = Executors.newFixedThreadPool(num);
        for (int i = 0; i < num; i++) {
            pool.execute(()->{
                for (int j = 0; j < 10000; j++) {
                    atomicInteger.getAndAdd(1);
                }
                countDownLatch.countDown();
            });
        }
        pool.shutdown();
        countDownLatch.await();
        System.out.println(atomicInteger.get());
    }
}
