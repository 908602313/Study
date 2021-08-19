package com.project.concurrent.myBlockingQueue;

import com.tools.Tools;

import java.util.Random;
import java.util.concurrent.*;

public class Test {


    public static void main(String[] args) {
        test3();
  }

    /**
     * 测试打断
     */
    private static void test3() {
        MyLinkedBlockingQueue<Integer> blockingQueue = new MyLinkedBlockingQueue<>();
        Thread thread = new Thread(() -> {
            Integer take = 0;
            try {
                 take = blockingQueue.take();
                System.out.println(take);
                System.out.println("我好了？");
            } catch (InterruptedException e) {
                System.out.println(take);
                System.out.println("根本没好");
            }
        });
        thread.start();
        Tools.sleep(200);
        thread.interrupt();
    }

    /**
     * 两生产者，一个消费者，测试限时入队出队是否正确。
     */
    private static void test2() {
        MyLinkedBlockingQueue<Integer> blockingQueue = new MyLinkedBlockingQueue<>(5);
        ExecutorService pool = Executors.newFixedThreadPool(3);
        Random random = new Random();
        pool.execute(()->{
            for (int i = 0; i < 1000; i++) {
                boolean success = false;
                try {
                    success = blockingQueue.offer(i, 1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (success)
                    System.out.println(i+":注入成功");
                else
                    System.out.println(i+":注入失败");
                Tools.sleep(random.nextInt(500));
            }
        });
        pool.execute(() -> {
            for (int i = 0; i < 1000; i++) {
                Integer val = null;
                try {
                    val = blockingQueue.poll(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (val != null)
                    System.out.println("输出成功");
                else
                    System.out.println("输出失败");
                Tools.sleep(random.nextInt(5000));
            }
        });
        pool.shutdown();
    }

    /**
     * 一个生产者，两个消费者，生产速度快，阻塞队列大小为10
     */
    private static void test1() {
        MyLinkedBlockingQueue<Integer> blockingQueue = new MyLinkedBlockingQueue<>(10);
        ExecutorService pool = Executors.newFixedThreadPool(3);
        Random random = new Random();
        pool.execute(() -> {
            for (int i = 0; i < 1000; i++) {
                try {
                    blockingQueue.put(i);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("注入：" + i);
                Tools.sleep(random.nextInt(500));
            }
        });
        for (int i = 0; i < 2; i++) {
            pool.execute(() -> {
                while (true) {
                    int take = 0;
                    try {
                        take = blockingQueue.take();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println(Thread.currentThread().getName() + "输出：" + take);
                    Tools.sleep(random.nextInt(1500));
                }
            });
        }
        pool.shutdown();
    }
}
