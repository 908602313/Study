package com.project.concurrent.myAtomicInteger;

public interface MyAtomicIntegerInterface {
    int get();

    void set(int newVal);

    boolean compareAndSet(int expected, int newVal);

    int addAndGet(int val);

    int getAndAdd(int val);
}
