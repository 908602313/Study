package com.project.concurrent.myAtomicInteger;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class MyAtomicInteger implements MyAtomicIntegerInterface {
    private volatile int val;
    private static final VarHandle VAL;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            VAL = lookup.findVarHandle(MyAtomicInteger.class, "val", int.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }


    public MyAtomicInteger(int val) {
        this.val = val;
    }

    public MyAtomicInteger() {
    }

    @Override
    public int get() {
        return val;
    }

    @Override
    public void set(int newVal) {
        this.val = newVal;
    }

    @Override
    public boolean compareAndSet(int expected, int newVal) {
        return VAL.compareAndSet(this, expected, newVal);
    }

    @Override
    public int addAndGet(int val) {
        while (true) {
            int oldVal = this.val, newVal = oldVal + val;
            if (compareAndSet(oldVal, newVal))
                return newVal;
        }
    }

    @Override
    public int getAndAdd(int val) {
        while (true) {
            int oldVal = this.val, newVal = oldVal + val;
            if (compareAndSet(oldVal, newVal))
                return oldVal;
        }
    }
}
