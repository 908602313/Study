package com.project.concurrent.myReentrantReadWriteLock;

import com.project.concurrent.myAQS.MyAbstractQueuedSynchronizer;
import com.project.concurrent.myAQS.MyConditionInterface;

public class MyReentrantReadWriteLock {
    private final Sync sync = new Sync();
    private final WriteLock writeLock = new WriteLock();
    private final ReadLock readLock = new ReadLock();

    public MyLockInterface readLock() {
        return readLock;
    }

    public MyLockInterface writeLock() {
        return writeLock;
    }

    public final class WriteLock implements MyLockInterface {
        @Override
        public void lock() {
            sync.acquire(1);
        }

        @Override
        public boolean tryLock() {
            return sync.tryAcquire(1);
        }

        @Override
        public void unlock() {
            sync.release(1);
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireInterruptibly(1);
        }
        @Override
        public MyConditionInterface newCondition() {
            return sync.newCondition();
        }
    }

    public final class ReadLock implements MyLockInterface {
        @Override
        public void lock() {
            sync.acquireShared(1);
        }

        @Override
        public boolean tryLock() {
            return sync.tryAcquireShared(1);
        }

        @Override
        public void unlock() {
            sync.releaseShared(1);
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireSharedInterruptibly(1);
        }

        @Override
        public MyConditionInterface newCondition() {
            throw new UnsupportedOperationException();
        }
    }

    private static final class Sync extends MyAbstractQueuedSynchronizer {
        private static final int SHIFT = 14;
        private static final int MAX_EXCLUSIVE_STATE = (1 << SHIFT) - 1;
        private static final int MAX_SHARED_STATE = (1 << (32 - SHIFT)) - 1;

        private final ThreadLocal<Integer> sharedHoldCount = new ThreadLocal<>();

        private int getSharedState(int state) {
            return state >>> SHIFT;
        }

        private int getExclusiveState(int state) {
            return state & MAX_EXCLUSIVE_STATE;
        }

        @Override
        protected boolean tryAcquire(int arg) {
            Thread currentThread = Thread.currentThread();
            int state = getState(), nextState = state + arg;
            if (state == 0) {
                if (compareAndSetState(state, nextState)) {
                    setExclusiveHeldThread(currentThread);
                    return true;
                }
                return false;
            }
            if (getSharedState(state) != 0) return false;
            if (!heldExclusive(currentThread)) return false;
            if (getExclusiveState(state) + arg >= MAX_EXCLUSIVE_STATE)
                throw new Error("超出写锁最大可重入数");
            setState(nextState);
            return true;
        }

        @Override
        protected boolean tryAcquireShared(int arg) {
            Thread thread = Thread.currentThread();
            int sharedHoldCount = getSharedHoldCount();
            while (true) {
                int state = getState(), nextState = state + (arg << SHIFT);
                if (getSharedState(state) + arg >= MAX_SHARED_STATE)
                    throw new Error("超出读锁最大可重入数");
                if (getExclusiveState(state) != 0) {
                    if (heldExclusive(thread)) {
                        addSharedHoldCount(arg);
                        setState(state + nextState);
                        return true;
                    }
                    return false;
                }
                if (sharedHoldCount == 0 && firstWaiterIsExclusive())
                    return false;
                if (compareAndSetState(state, nextState)) {
                    addSharedHoldCount(arg);
                    return true;
                }
            }
        }

        private void checkSharedHoldCount(int arg) {
            if (getSharedHoldCount() <= arg)
                throw new IllegalMonitorStateException("当前线程并不持有共享锁");
        }

        private void addSharedHoldCount(int arg) {
            sharedHoldCount.set(getSharedHoldCount() + arg);
        }

        @Override
        protected boolean tryRelease(int arg) {
            checkHeldExclusive(Thread.currentThread());
            int nextState = getState() - arg;
            setState(nextState);
            boolean released = nextState == 0;
            if (released) setExclusiveHeldThread(null);
            return released;
        }

        @Override
        protected boolean tryReleaseShared(int arg) {
            checkSharedHoldCount(arg);
            addSharedHoldCount(-arg);
            while (true) {
                int state = getState(), nextState = state - (arg << SHIFT);
                if (compareAndSetState(state, nextState))
                    return nextState == 0;
            }
        }

        private int getSharedHoldCount() {
            Integer ans = sharedHoldCount.get();
            return ans == null ? 0 : ans;
        }

        MyConditionInterface newCondition() {
            return new MyCondition();
        }
    }
}
