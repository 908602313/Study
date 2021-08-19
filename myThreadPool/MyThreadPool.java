package com.project.concurrent.myThreadPool;

import com.project.concurrent.myAtomicInteger.MyAtomicInteger;
import com.project.concurrent.myBlockingQueue.MyBlockingQueueInterface;
import com.project.concurrent.myBlockingQueue.MyLinkedBlockingQueue;

import java.util.HashSet;
import java.util.IllegalFormatCodePointException;
import java.util.concurrent.*;
import java.util.function.Supplier;

public class MyThreadPool implements MyThreadPoolInterface {


    private final MyAtomicInteger stateAndCount = new MyAtomicInteger(stateAndCountAt(RUNNING, 0));

    private static final int COUNT_BITS = Integer.SIZE - 2;
    private static final int COUNT_MASK = (1 << COUNT_BITS) - 1;
    private static final int STATE_MASK = ~COUNT_MASK;
    private static final int RUNNING = 2 << COUNT_BITS;
    private static final int SHUTDOWN = 3<< COUNT_BITS;
    private static final int SHUTDOWN_NOW = 0 << COUNT_BITS;
    private static final int TERMINATED = 1 << COUNT_BITS;


    private static int stateAndCountAt(int state, int count) {
        return state | count;
    }

    private static int stateAt(int stateAndCount) {
        return stateAndCount & STATE_MASK;
    }

    private static int countAt(int stateAndCount) {
        return stateAndCount & COUNT_MASK;
    }


    private final HashSet<Worker> workers = new HashSet<>();

    private final int corePoolSize;

    private final int maximumPoolSize;

    private final long keepAliveTime;

    private final MyBlockingQueueInterface<Runnable> workQueue;


    private final MyRejectedExecutionHandlerInterface handler;


    public MyThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit timeUnit) {
        this(corePoolSize,
                maximumPoolSize,
                keepAliveTime, timeUnit,
                new MyLinkedBlockingQueue<>(),
                new MyAbortRejectedHandler());
    }

    public MyThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit timeUnit, MyBlockingQueueInterface<Runnable> workQueue) {
        this(corePoolSize,
                maximumPoolSize,
                keepAliveTime, timeUnit,
                workQueue,
                new MyAbortRejectedHandler());
    }

    public MyThreadPool(int corePoolSize,
                        int maximumPoolSize,
                        long keepAliveTime,
                        TimeUnit timeUnit,
                        MyBlockingQueueInterface<Runnable> workQueue,
                        MyRejectedExecutionHandlerInterface handler) {
        if (corePoolSize < 0 || maximumPoolSize < corePoolSize || keepAliveTime < 0)
            throw new IllegalArgumentException();
        if (workQueue == null || handler == null)
            throw new NullPointerException();
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.keepAliveTime = timeUnit.toNanos(keepAliveTime);
        this.workQueue = workQueue;
        this.handler = handler;
    }
    static final class MyDelayRejectedHandler implements MyRejectedExecutionHandlerInterface {
        @Override
        public void rejectedExecution(Runnable task, MyThreadPool pool) {
            MyBlockingQueueInterface<Runnable> queue = pool.getQueue();
            final String message = pool.toString() + "中任务：" + task.toString() + "被拒绝。";
            try {
                if (!pool.isShutDown()) {
                    queue.offer(task, 60, TimeUnit.SECONDS);
                    if (pool.isShutDownNow() && queue.remove(task))
                        throw new RejectedExecutionException(message);
                } else
                    throw new RejectedExecutionException(message);
            } catch (InterruptedException e) {
                throw new RejectedExecutionException(message);
            }
        }
    }
    static final class MyAbortRejectedHandler implements MyRejectedExecutionHandlerInterface{
        @Override
        public void rejectedExecution(Runnable task, MyThreadPool pool) {
            throw new RejectedExecutionException( pool.toString() + "中任务：" + task.toString() + "被拒绝。");
        }
    }


    private final class Worker extends Thread {
        private final MyAtomicInteger state = new MyAtomicInteger(BUSY);
        private static final int IDLE = 0;
        private static final int BUSY = 1;


        private Runnable firstTask;

        public Worker(Runnable firstTask) {
            this.firstTask = firstTask;
        }

        public void lock() {
            while (!tryLock()) ;
        }

        public void unlock() {
            state.set(IDLE);
        }

        public boolean tryLock() {
            return state.compareAndSet(IDLE, BUSY);
        }

        @Override
        public void run() {
            Runnable task = firstTask;
            firstTask = null;
            unlock();
            boolean throwable = true;
            try {
                while (task != null || (task = getTask()) != null) {
                    lock();
                    if (stateAt(stateAndCount.get()) >= SHUTDOWN_NOW)
                        this.interrupt();
                    try {
                        task.run();
                    } finally {
                        unlock();
                        task = null;
                    }
                }
                throwable = false;
            } finally {
                if (throwable) {
                    stateAndCount.addAndGet(-1);
                    addWorker(null, false);
                }
                workers.remove(this);
                if (stateAndCount.get()>=SHUTDOWN&&workers.size()==0)
                    stateAndCount.set(TERMINATED);
            }

        }
    }




    @Override
    public void execute(Runnable task) {
        if (task == null) throw new NullPointerException();
        int sc = stateAndCount.get();
        if (sc >= SHUTDOWN)
            reject(task);
        if (countAt(sc) < corePoolSize && addWorker(task, true))
            return;
        sc = stateAndCount.get();
        if (sc < SHUTDOWN && workQueue.offer(task)) {
            if (stateAndCount.get() >= SHUTDOWN_NOW && workQueue.remove(task))
                reject(task);
            return;
        }
        sc = stateAndCount.get();
        if (countAt(sc) < maximumPoolSize && addWorker(task, false))
            return;
        reject(task);
    }

    @Override
    public <E> MyFutureTask<E> submit(Supplier<E> supplier) {
        if (supplier == null) throw new NullPointerException();
        MyFutureTask<E> futureTask = new MyFutureTask<>(supplier);
        execute(futureTask);
        return futureTask;
    }

    @Override
    public void shutdown() {
        while (true) {
            int sc = stateAndCount.get();
            if (sc >= SHUTDOWN)
                return;
            if (stateAndCount.compareAndSet(sc, stateAndCountAt(SHUTDOWN, countAt(sc)))) {
                for (Worker worker : workers) {
                    if (!worker.isInterrupted() && worker.tryLock()) {
                        try {
                            worker.interrupt();
                        } finally {
                            worker.unlock();
                        }
                    }
                }
                return;
            }
        }
    }

    @Override
    public MyBlockingQueueInterface<Runnable> shutdownNow() {
        while (true) {
            int sc = stateAndCount.get();
            if (sc >= SHUTDOWN_NOW)
                return workQueue;
            if (stateAndCount.compareAndSet(sc, stateAndCountAt(SHUTDOWN_NOW, countAt(sc)))) {
                for (Worker worker : workers) {
                    if (!worker.isInterrupted())
                        worker.interrupt();
                }
            }
            return workQueue;
        }
    }


    public MyBlockingQueueInterface<Runnable> getQueue() {
        return workQueue;
    }

    private boolean addWorker(Runnable firstTask, boolean core) {
        int max = core ? corePoolSize : maximumPoolSize;
        while (true) {
            int sc = stateAndCount.get(), state = stateAt(sc);
            if (countAt(sc) >= max ||
                    state >= SHUTDOWN_NOW ||
                    state >= SHUTDOWN && (workQueue.isEmpty() || firstTask != null))
                return false;
            if (stateAndCount.compareAndSet(sc, sc + 1)) {
                Worker worker = new Worker(firstTask);
                worker.start();
                workers.add(worker);
                return true;
            }
        }
    }

    private Runnable getTask() {
        boolean timeOut = false;
        while (true) {
            int sc = stateAndCount.get();
            if (sc >= SHUTDOWN_NOW || sc >= SHUTDOWN && workQueue.isEmpty()) {
                stateAndCount.addAndGet(-1);
                return null;
            }
            if (timeOut && countAt(sc) > corePoolSize) {
                if (stateAndCount.compareAndSet(sc, sc - 1))
                    return null;
                continue;
            }
            boolean timeLimited = countAt(sc) > corePoolSize;
            try {
                Runnable task = timeLimited ? workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) : workQueue.take();
                if (task != null)
                    return task;
                timeOut = true;
            } catch (InterruptedException e) {
                timeOut = false;
            }
        }
    }

    public boolean isShutDown() {
        return stateAndCount.get() >= SHUTDOWN;
    }

    public boolean isShutDownNow() {
        return stateAndCount.get() >= SHUTDOWN_NOW;
    }

    public boolean isTerminated() {
        return stateAndCount.get()==TERMINATED;
    }


    private void reject(Runnable task) {
        handler.rejectedExecution(task, this);
    }
}
