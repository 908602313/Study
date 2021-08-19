package com.project.concurrent.myBlockingQueue;

import java.util.concurrent.TimeUnit;

public interface MyBlockingQueueInterface<E> {
    /**
     * 一直阻塞
     */
    void put(E e) throws InterruptedException;

    /**
     * 超时退出
     */
    boolean offer(E e, long time, TimeUnit timeUnit) throws InterruptedException;

    /**
     *阻塞即返回
     */
    boolean offer(E e);

    /**
     * 一直阻塞
     */
    E take() throws InterruptedException;

    /**
     * 立即退出，返回元素或null
     */
    E poll();

    /**
     * 超时退出
     */
    E poll(long time, TimeUnit timeUnit) throws InterruptedException;

    /**
     * 返回头部元素
     */
    E peek();

    boolean remove(Object o);

    boolean isEmpty();

    int size();
}
