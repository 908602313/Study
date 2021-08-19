package com.project.concurrent.myAQS;

import java.util.concurrent.TimeUnit;

public interface MyConditionInterface {
    void await()throws InterruptedException ;
    boolean await(long time, TimeUnit unit)throws InterruptedException;
    boolean await(long nanoTime)throws InterruptedException;

    void signal();
    void signalAll();
}
