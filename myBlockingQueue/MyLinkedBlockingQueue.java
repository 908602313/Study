package com.project.concurrent.myBlockingQueue;

import com.project.concurrent.myAQS.MyConditionInterface;
import com.project.concurrent.myAtomicInteger.MyAtomicInteger;
import com.project.concurrent.myReentrantReadWriteLock.MyLockInterface;
import com.project.concurrent.myReentrantReadWriteLock.MyReentrantReadWriteLock;

import java.util.concurrent.TimeUnit;

public class MyLinkedBlockingQueue<E> implements MyBlockingQueueInterface<E> {
    private final MyAtomicInteger size = new MyAtomicInteger();

    private final int MAX_SIZE;

    private Node<E> head = new Node<>(null);

    private Node<E> tail = head;

    private final MyLockInterface putLock = new MyReentrantReadWriteLock().writeLock();

    private final MyLockInterface takeLock = new MyReentrantReadWriteLock().writeLock();

    private final MyConditionInterface full = putLock.newCondition();

    private final MyConditionInterface empty = takeLock.newCondition();


    public MyLinkedBlockingQueue() {
        this(Integer.MAX_VALUE);
    }

    public MyLinkedBlockingQueue(int maxSize) {
        if (maxSize <= 0) throw new IllegalArgumentException();
        this.MAX_SIZE = maxSize;
    }

    /**
     * 一直阻塞
     */
    @Override
    public void put(E e) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        MyLockInterface putLock = this.putLock;
        MyAtomicInteger size = this.size;
        MyConditionInterface full = this.full;
        int len;
        putLock.lockInterruptibly();
        try {
            while (size.get() == MAX_SIZE)
                full.await();
            enq(new Node<>(e));
            len = size.addAndGet(1);
            if (len < MAX_SIZE)
                full.signal();
        } finally {
            putLock.unlock();
        }
        if (len == 1)
            signalEmpty();
    }

    @Override
    public boolean offer(E e, long time, TimeUnit timeUnit) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        if (time <= 0) throw new IllegalArgumentException("time<=0");
        MyLockInterface putLock = this.putLock;
        MyAtomicInteger size = this.size;
        MyConditionInterface full = this.full;
        long remaining = timeUnit.toNanos(time), deadLine = System.nanoTime() + remaining;
        int len;
        putLock.lockInterruptibly();
        try {
            while (size.get() == MAX_SIZE) {
                if (remaining <= 0L)
                    return false;
                full.await(remaining);
                remaining = deadLine - System.nanoTime();

            }
            enq(new Node<>(e));
            len = size.addAndGet(1);
            if (len < MAX_SIZE)
                full.signal();

        } finally {
            putLock.unlock();
        }
        if (len == 1)
            signalEmpty();
        return true;
    }

    @Override
    public boolean offer(E e) {
        if (e == null) throw new NullPointerException();
        MyLockInterface putLock = this.putLock;
        MyAtomicInteger size = this.size;
        putLock.lock();
        boolean success = false;
        int len = -1;
        try {
            if (size.get() < MAX_SIZE) {
                enq(new Node<>(e));
                len = size.addAndGet(1);
                if (len < MAX_SIZE)
                    full.signal();
                success = true;
            }
        } finally {
            putLock.unlock();
        }
        if (len == 1)
            signalEmpty();
        return success;
    }

    @Override
    public E take() throws InterruptedException {
        MyLockInterface takeLock = this.takeLock;
        MyAtomicInteger size = this.size;
        MyConditionInterface empty = this.empty;
        E e;
        int len;
        takeLock.lockInterruptibly();
        try {
            while (size.get() == 0)
                empty.await();
            e = deq();
            len = size.addAndGet(-1);
            if (len > 0)
                empty.signal();
        } finally {
            takeLock.unlock();
        }
        if (len == MAX_SIZE - 1)
            signalFull();
        return e;
    }

    @Override
    public E poll() {
        MyLockInterface takeLock = this.takeLock;
        MyAtomicInteger size = this.size;
        MyConditionInterface empty = this.empty;
        E e = null;
        int len = -1;
        takeLock.lock();
        try {
            if (size.get() > 0) {
                e = deq();
                len = size.addAndGet(-1);
                if (len > 0)
                    empty.signal();
            }
        } finally {
            takeLock.unlock();
        }
        if (len == MAX_SIZE - 1)
            signalFull();
        return e;
    }

    @Override
    public E poll(long time, TimeUnit timeUnit) throws InterruptedException {
        if (time <= 0) throw new IllegalArgumentException("time<=0");
        MyLockInterface takeLock = this.takeLock;
        MyAtomicInteger size = this.size;
        MyConditionInterface empty = this.empty;
        long remaining = timeUnit.toNanos(time), deadLine = System.nanoTime() + remaining;
        E e;
        int len;
        takeLock.lockInterruptibly();
        try {
            while (size.get() == 0) {
                if (remaining <= 0L)
                    return null;
                empty.await(remaining);
                remaining = deadLine - System.nanoTime();
            }
            e = deq();
            len = size.addAndGet(-1);
            if (len > 0)
                empty.signal();

        } finally {
            takeLock.unlock();
        }
        if (len == MAX_SIZE - 1)
            signalFull();
        return e;
    }

    @Override
    public E peek() {
        Node<E> headNext = head.next;
        return headNext == null ? null : headNext.e;
    }

    @Override
    public boolean remove(Object o) {
        if (o == null)
            return false;
        fullLock();
        try {
            for (Node<E> node = head.next, pre = head; node != null; pre = node, node = node.next) {
                if (o.equals(node.e)) {
                    pre.next = node.next;
                    if (node == tail)
                        tail = pre;
                    if (size.getAndAdd(-1) == MAX_SIZE)
                        full.signal();
                    return true;
                }
            }
        } finally {
            fullUnlock();
        }

        return false;
    }

    @Override
    public boolean isEmpty() {
        return size.get() == 0;
    }

    @Override
    public int size() {
        return size.get();
    }

    private void signalFull() {
        MyLockInterface putLock = this.putLock;
        putLock.lock();
        try {
            full.signal();
        } finally {
            putLock.unlock();
        }
    }

    private void signalEmpty() {
        MyLockInterface takeLock = this.takeLock;
        takeLock.lock();
        try {
            empty.signal();
        } finally {
            takeLock.unlock();
        }
    }

    private void enq(Node<E> node) {
        tail = tail.next = node;
    }

    private E deq() {
        Node<E> next = head.next;
        E e = next.e;
        next.e = null;
        head = next;
        return e;
    }

    protected final void fullLock() {
        putLock.lock();
        takeLock.lock();
    }

    protected final  void fullUnlock() {
        takeLock.unlock();
        putLock.unlock();
    }

    protected final MyConditionInterface newPutCondition() {
        return putLock.newCondition();
    }

    protected final MyConditionInterface newTakeCondition() {
        return takeLock.newCondition();
    }

    protected final void putLock() {
        putLock.lock();
    }

    protected final void putUnlock() {
        putLock.unlock();
    }

    protected final void takeLock() {
        takeLock.lock();
    }

    protected final void takeUnlock() {
        takeLock.unlock();
    }

    private final static class Node<E> {
        E e;
        Node<E> next;

        public Node(E e) {
            this.e = e;
        }
    }
}
