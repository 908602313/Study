package com.project.concurrent.myThreadPool;

import com.project.concurrent.myBlockingQueue.MyBlockingQueueInterface;
import com.project.concurrent.myBlockingQueue.MyLinkedBlockingQueue;

import java.util.List;
import java.util.function.Supplier;

public interface MyThreadPoolInterface {
    void execute(Runnable task);

    <E> MyFutureTask<E> submit(Supplier<E> supplier);

    void shutdown();

    MyBlockingQueueInterface<Runnable> shutdownNow();
}
