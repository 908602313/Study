package com.project.concurrent.other;

import com.project.concurrent.myAQS.MyAbstractQueuedSynchronizer;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;

public class MySemaphore {

    private final Sync sync;

    public MySemaphore(int permits) {
        this.sync = new Sync(permits);
    }

    public void acquire() {
        sync.acquireShared(1);
    }

    public void release() {
        sync.releaseShared(1);
    }

    public int getPermits() {
        return sync.getPermits();
    }

    private static class Sync extends MyAbstractQueuedSynchronizer {
        public Sync(int state) {
            setState(state);
        }

        public int getPermits() {
            return getState();
        }

        @Override
        protected boolean tryAcquireShared(int arg) {
            while (true) {
                int state = getState(), nextState = state - arg;
                if (nextState < 0)
                    return false;
                if (compareAndSetState(state, nextState))
                    return true;
            }
        }

        @Override
        protected boolean tryReleaseShared(int arg) {
            while (true) {
                int state = getState(), nextState = state + arg;
                if (nextState < state)
                    throw new Error("超过最大可获取量");
                if (compareAndSetState(state, nextState))
                    return nextState > 0;
            }
        }
    }
}
