package com.project.concurrent.myThreadPool;

import com.project.concurrent.myAQS.MyConditionInterface;
import com.project.concurrent.myAtomicInteger.MyAtomicInteger;
import com.project.concurrent.myBlockingQueue.MyLinkedBlockingQueue;
import com.project.concurrent.myReentrantReadWriteLock.MyLockInterface;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class MyFutureTask<E> implements MyFutureTaskInterface<E>, Runnable {
    private final MyGetBlockingQueue<E> result = new MyGetBlockingQueue<>();

    private final Supplier<E> supplier;

    public MyFutureTask(Supplier<E> supplier) {
        this.supplier = supplier;
    }


    @Override
    public E get() throws InterruptedException {
        return result.get();
    }

    @Override
    public E get(long time, TimeUnit timeUnit) throws InterruptedException {
        if (time <= 0) throw new IllegalArgumentException();
        return result.get(time, timeUnit);
    }

    @Override
    public void run() {
        result.offer(supplier.get());
    }

    private static final class MyGetBlockingQueue<E> extends MyLinkedBlockingQueue<E> {
        public MyGetBlockingQueue() {
            super(1);
        }

        private final MyConditionInterface getCondition = super.newTakeCondition();

        public E get() throws InterruptedException {
            E res = peek();
            if (res != null)
                return res;
            takeLock();
            try {
                while (true) {
                    getCondition.await();
                    res = peek();
                    if (res != null)
                        return res;
                }
            } finally {
                takeUnlock();
            }
        }

        public E get(long time, TimeUnit timeUnit) throws InterruptedException {
            E res = peek();
            if (res != null)
                return res;
            long remaining = timeUnit.toNanos(time), deadLine = remaining + System.nanoTime();
            takeLock();
            try {
                while (true) {
                    getCondition.await(remaining);
                    res = peek();
                    if (res != null)
                        return res;
                    remaining = deadLine - System.nanoTime();
                    if (remaining <= 0)
                        return null;
                }
            } finally {
                takeUnlock();
            }
        }

        @Override
        public boolean offer(E e) {
            boolean success = super.offer(e);
            takeLock();
            try {
                getCondition.signalAll();
            } finally {
                takeUnlock();
            }
            return success;
        }
    }
}
