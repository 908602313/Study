package com.project.concurrent.other;

import com.project.concurrent.myAtomicInteger.MyAtomicInteger;
import com.project.concurrent.other.MyCountDownLatch;
import com.project.concurrent.other.MyCyclicBarrier;
import com.project.concurrent.other.MySemaphore;
import com.tools.Tools;

import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

public class Test {
    static final ThreadLocal<Integer> sharedHoldCount = new ThreadLocal<>();


    public static void main(String[] args) throws InterruptedException {
       MyCyclicBarrierTest.test1();
    }
}

class MySemaphoreTest {
    /**
     * 完成三个线程的有序执行
     */
    public static void test1() {
        MySemaphore s1 = new MySemaphore(0);
        MySemaphore s2 = new MySemaphore(0);
        MySemaphore s3 = new MySemaphore(1);
        int loop = 10;
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < loop; i++) {
                s3.acquire();
                System.out.println(1);
                s1.release();
            }
        }, "t1");
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < loop; i++) {
                s1.acquire();
                System.out.println(2);
                s2.release();
            }
        }, "t2");
        Thread t3 = new Thread(() -> {
            for (int i = 0; i < loop; i++) {
                s2.acquire();
                System.out.println(3);
                s3.release();
            }
        }, "t3");
        t1.start();
        t2.start();
        t3.start();
    }

    public static void test2() {
        MySemaphore s1 = new MySemaphore(0);
        Thread t1 = new Thread(() -> {
            s1.acquire();
            System.out.println(Thread.currentThread().getName());
        }, "t1");
        Thread t2 = new Thread(() -> {
            s1.acquire();
            System.out.println(Thread.currentThread().getName());
        }, "t2");
        Thread t3 = new Thread(() -> {
            s1.acquire();
            System.out.println(Thread.currentThread().getName());
        }, "t3");
        t1.start();
        t2.start();
        t3.start();
        for (int i = 0; i < 3; i++) {
            Tools.sleep(500);
            s1.release();
        }
    }
}

class MyCyclicBarrierTest {
    /**
     * 五个线程，四个计数，测试是否正常工作。
     */
    public static void test1() {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        MyCyclicBarrier cyclicBarrier = new MyCyclicBarrier(4);
        MyAtomicInteger integer = new MyAtomicInteger();
//        CyclicBarrier cyclicBarrier = new CyclicBarrier(4);
        Random random = new Random();
        for (int i = 0; i < 4*150; i++) {
            for (int j = 0; j <4; j++) {
                pool.execute(() -> {
                    Tools.sleep(random.nextInt(10));
                    int order = 0;
                    try {
                        order = cyclicBarrier.await();
//                        System.out.println(integer.addAndGet(1));
                    } catch (InterruptedException  e) {
                        e.printStackTrace();
                    }
                    System.out.println(Thread.currentThread().getName() + ",在" + System.currentTimeMillis() + "，好了,是第" + order);
                });
            }
        }
//        System.out.println(integer.get());
        pool.shutdown();
    }

    /**
     * 与jdk自带的进行性能对比
     */
    public static void test2() throws InterruptedException {
        int nThreads = 20,barriers = 4, loop = 1000, count = loop * nThreads,randomBound = 10;
        ExecutorService pool = Executors.newFixedThreadPool(nThreads);
        CyclicBarrier cyclicBarrier = new CyclicBarrier(barriers);
        CountDownLatch countDownLatch = new CountDownLatch(count);
        Random random = new Random();
        long start = System.currentTimeMillis();
        for (int i = 0; i < loop; i++) {
            for (int j = 0; j < nThreads; j++) {
                pool.execute(() -> {
                    Tools.sleep(random.nextInt(randomBound));
                    countDownLatch.countDown();
                    try {
                        int order = cyclicBarrier.await();
                    } catch (InterruptedException | BrokenBarrierException e) {
                        e.printStackTrace();
                    }
                });
            }
        }
        countDownLatch.await();
        System.out.println("jdk:"+(System.currentTimeMillis() - start));

        MyCyclicBarrier myCyclicBarrier = new MyCyclicBarrier(barriers);
        CountDownLatch countDownLatch0 = new CountDownLatch(count);
        start = System.currentTimeMillis();
        for (int i = 0; i < loop; i++) {
            for (int j = 0; j < nThreads; j++) {
                pool.execute(() -> {
                    Tools.sleep(random.nextInt(randomBound));
                    countDownLatch0.countDown();
                    try {
                        int order = myCyclicBarrier.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            }
        }
        countDownLatch0.await();
        System.out.println("zc:"+(System.currentTimeMillis() - start));

        pool.shutdown();
    }

}

class MyCountDownLatchTest {
    static MyCountDownLatch count = new MyCountDownLatch(3);

    public static void test1() {

        new Thread(() -> {
            count.await();
            System.out.println(Thread.currentThread().getName() + "好了");
        }).start();
        new Thread(() -> {
            count.await();
            System.out.println(Thread.currentThread().getName() + "好了");
        }).start();
        Tools.sleep(50);
        new Thread(() -> count.countDown()).start();
        new Thread(() -> count.countDown()).start();
        Tools.sleep(50);
    }
}