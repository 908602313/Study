package com.project.concurrent.myReentrantReadWriteLock;

import com.project.concurrent.myAQS.MyConditionInterface;

public interface MyLockInterface {
    void lock();
    boolean tryLock();
    void unlock();
    void lockInterruptibly() throws InterruptedException;
    MyConditionInterface newCondition();
}
