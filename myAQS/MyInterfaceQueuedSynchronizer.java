package com.project.concurrent.myAQS;

public interface MyInterfaceQueuedSynchronizer {
    void acquire(int arg);

    void acquireSharedInterruptibly(int arg) throws InterruptedException;

    void acquireShared(int arg);

    void acquireInterruptibly(int arg) throws InterruptedException;

    boolean release(int arg);

    boolean releaseShared(int arg);
}
