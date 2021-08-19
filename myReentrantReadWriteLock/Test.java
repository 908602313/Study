package com.project.concurrent.myReentrantReadWriteLock;

import com.project.concurrent.myAQS.MyConditionInterface;
import com.project.concurrent.myBlockingQueue.MyLinkedBlockingQueue;
import com.tools.Tools;

import java.util.concurrent.*;
import java.util.function.Supplier;

public class Test {
    static boolean end = false;

    public static void main(String[] args) {
       test6();
    }


    /**
     * 测试终结节点是否可以被正确回收
     */
    private static void test6() {
        MyReentrantReadWriteLock lock = new MyReentrantReadWriteLock();
        MyLockInterface readLock = lock.readLock();
        MyLockInterface writeLock = lock.writeLock();
        new Thread(() -> {
            readLock.lock();
            try {
                System.out.println(Thread.currentThread().getName() + "锁上了思密达");
                Tools.sleep(500 * 1000);
            } finally {
                readLock.unlock();
            }
        }).start();
        Thread thread = new Thread(() -> {
            for (int i = 0; i < 500000; i++) {
                try {
                    writeLock.lockInterruptibly();
                    try {
                        System.out.println(Thread.currentThread().getName() + "锁上了思密达");
                    } finally {
                        writeLock.unlock();
                    }
                } catch (InterruptedException e) {
                    System.out.println("等待锁被打断思密达第" + i + "次");
                }
                Tools.sleep(200);
            }
        });
        thread.start();
        Tools.sleep(1000);
        for (int i = 0; i < 500000; i++) {
            thread.interrupt();
            Tools.sleep(200);
            if (i % 100 == 0)
                System.gc();
        }

    }

    /**
     * 两线程测试读写锁是否可被打断
     */
    private static void test5() {
        MyReentrantReadWriteLock lock = new MyReentrantReadWriteLock();
        MyLockInterface readLock = lock.readLock();
        MyLockInterface writeLock = lock.writeLock();
        new Thread(() -> {
            readLock.lock();
            try {
                System.out.println(Thread.currentThread().getName() + "锁上了读锁思密达");
                Tools.sleep(2000);
            } finally {
                readLock.unlock();
            }
        }).start();
        Thread thread = new Thread(() -> {
            try {
                writeLock.lockInterruptibly();
                try {
                    System.out.println(Thread.currentThread().getName() + "锁上了思密达");
                } finally {
                    writeLock.unlock();
                }
            } catch (InterruptedException e) {
                System.out.println(Thread.currentThread().getName() +"等待写锁被打断思密达");
            }
        });
        thread.start();
        Tools.sleep(1000);
        thread.interrupt();

    }

    /**
     * 测试写锁的限时等待是否正常工作
     */
    private static void test4() {
        int numOfThread = 5;
        MyReentrantReadWriteLock lock = new MyReentrantReadWriteLock();
        MyLockInterface writeLock = lock.writeLock();
        MyConditionInterface condition = writeLock.newCondition();
        ExecutorService pool = Executors.newFixedThreadPool(numOfThread);
        for (int i = 0; i < numOfThread; i++) {
            pool.execute(() -> {
                boolean timeout = false;
                writeLock.lock();
                try {
                    System.out.println(Thread.currentThread() + ":进去了思密达");
                    try {
                        timeout = condition.await(2, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println(Thread.currentThread() + ":出来了思密达");
                } finally {
                    if (timeout)
                        System.out.println("没等到思密达");
                    writeLock.unlock();
                }
            });
        }
        Tools.sleep(1000);
        System.out.println("--------------------");
        pool.shutdown();
    }

    /**
     * 7个线程测试读写锁是否正常工作，是否会导致写进程饥饿。
     */
    private static void test3() {
        MyReentrantReadWriteLock lock = new MyReentrantReadWriteLock();
        MyLockInterface readLock = lock.readLock();
        MyLockInterface writeLock = lock.writeLock();

        ExecutorService pool = Executors.newFixedThreadPool(7);
        for (int i = 0; i < 2; i++) {
            pool.execute(() -> {
                readLock.lock();
                try {
                    System.out.println(Thread.currentThread().getName() + "：读者进来了思密达");
                    Tools.sleep(2000);
                    System.out.println(Thread.currentThread().getName() + "：读者出来了思密达");

                } finally {
                    readLock.unlock();
                }
            });
        }
        for (int i = 0; i < 2; i++) {
            pool.execute(() -> {
                writeLock.lock();
                try {
                    System.out.println(Thread.currentThread().getName() + ":写者进来了思密达");
                    Tools.sleep(4000);
                    System.out.println(Thread.currentThread().getName() + ":写者出来了思密达");
                } finally {
                    writeLock.unlock();
                }
            });
        }
        for (int i = 0; i < 3; i++) {
            pool.execute(() -> {
                readLock.lock();
                try {
                    System.out.println(Thread.currentThread().getName() + "：读者进来了思密达");
                    Tools.sleep(1000);
                    System.out.println(Thread.currentThread().getName() + "：读者出来了思密达");

                } finally {
                    readLock.unlock();
                }
            });
        }
        pool.shutdown();
    }

    /**
     * 五个线程测试写锁的condition
     */
    private static void test2() {
        int numOfThread = 5;
        MyReentrantReadWriteLock lock = new MyReentrantReadWriteLock();
        MyLockInterface writeLock = lock.writeLock();
        MyConditionInterface condition = writeLock.newCondition();
        ExecutorService pool = Executors.newFixedThreadPool(numOfThread);
        for (int i = 0; i < numOfThread; i++) {
            pool.execute(() -> {
                writeLock.lock();
                try {
                    System.out.println(Thread.currentThread() + ":进去了思密达");
                    try {
                        condition.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println(Thread.currentThread() + ":出来了思密达");
                } finally {
                    writeLock.unlock();
                }
            });
        }
        Tools.sleep(1000);
        System.out.println("-------------------------");
        writeLock.lock();
        try {
//            for (int i = 0; i < numOfThread; i++) {
//                condition.signal();
//            }
           condition.signalAll();
        } finally {
            writeLock.unlock();
        }
        pool.shutdown();
    }

    /**
     * 两线程测试读写锁是否正常工作
     */
    private static void test1() {
        MyReentrantReadWriteLock lock = new MyReentrantReadWriteLock();
        MyLockInterface readLock = lock.readLock();
        MyLockInterface writeLock = lock.writeLock();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        for (int i = 0; i < 2; i++) {
            pool.execute(() -> {
//                readLock.lock();
                writeLock.lock();
                try {
                    System.out.println(Thread.currentThread().getName() + "锁上了思密达");
                    Tools.sleep(2000);
                } finally {
                    System.out.println(Thread.currentThread().getName() + "放开了思密达");
//                    readLock.unlock();
                    writeLock.unlock();

                }
            });
        }
        pool.shutdown();
    }
}
