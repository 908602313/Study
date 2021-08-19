package com.project.concurrent.myThreadPool;

import com.project.concurrent.myAtomicInteger.MyAtomicInteger;
import com.project.concurrent.myBlockingQueue.MyLinkedBlockingQueue;
import com.project.concurrent.other.MyCountDownLatch;
import com.tools.Tools;

import java.util.Map;
import java.util.concurrent.*;

public class Test {
    public static void main(String[] args) {
        MyThreadPool pool = new MyThreadPool(1, 1, 0, TimeUnit.SECONDS, new MyLinkedBlockingQueue<>(10));

    }


    /**
     * 测试任务异常后线程池工作情况
     */
    private static void test3() {
        MyThreadPool pool = new MyThreadPool(1, 1, 0, TimeUnit.SECONDS, new MyLinkedBlockingQueue<>(10));
        pool.execute(()->{
            int a = 1/0;
        });
        for (int i = 0; i < 100; i++) {
            Tools.sleep(200);
            pool.execute(()->{
                System.out.println("执行任务思密达");
            });
        }


        pool.shutdown();
    }

    /**
     * 测试延时获取执行结果。
     */
    private static void test2() {
        MyThreadPool pool = new MyThreadPool(2, 4, 30, TimeUnit.SECONDS, new MyLinkedBlockingQueue<>(10));
        MyFutureTask<Integer> futureTask = pool.submit(() -> {
            int a = 5, b = 10;
            Tools.sleep(2000);
            return a * b;
        });
        new Thread(() -> {
            try {
                System.out.println("一号线程：" + futureTask.get(1, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }).start();
        new Thread(() -> {
            try {
                System.out.println("二号线程：" + futureTask.get(3, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }).start();
        new Thread(() -> {
            try {
                System.out.println("三号线程：" + futureTask.get());

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }).start();
        pool.shutdown();
    }

    /**
     * 测试线程池对多个任务执行的正确性
     */
    private static void test1() {
        MyAtomicInteger atomicInteger = new MyAtomicInteger();

        MyThreadPool pool = new MyThreadPool(2, 4, 30, TimeUnit.SECONDS, new MyLinkedBlockingQueue<>(10));
//        ThreadPoolExecutor pool = new ThreadPoolExecutor(2, 4, 30, TimeUnit.SECONDS,new LinkedBlockingQueue<>(10));
        int tasks = 20;

        try {
            for (int i = 0; i < tasks; i++) {
                int finalI = i;
                pool.execute(() -> {
                    Tools.sleep(500);
                    System.out.println(Thread.currentThread() + ":我好了," + finalI);
                    atomicInteger.addAndGet(1);
                });
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        pool.shutdown();
        Tools.sleep(5000);
//        System.out.println("pool.isShutDown:"+pool.isShutDown());
//        System.out.println("pool.isShutDownNow:"+pool.isShutDownNow());
        System.out.println("pool.isTerminated:" + pool.isTerminated());
        System.out.println(atomicInteger.get());
    }
}
