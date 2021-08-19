package com.project.concurrent.myThreadPool;

import java.util.concurrent.TimeUnit;

public interface MyFutureTaskInterface<E> {
    E get() throws InterruptedException;

    E get(long time, TimeUnit timeUnit) throws InterruptedException;
}
