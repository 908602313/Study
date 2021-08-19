package com.project.concurrent.other;

import com.project.concurrent.myAQS.MyAbstractQueuedSynchronizer;
import com.tools.Tools;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;

public class MyCountDownLatch {

    Sync sync;

    public MyCountDownLatch(int count) {
        if (count <= 0) throw new IllegalArgumentException("count<=0");
        this.sync = new Sync(count);
    }

    public void countDown() {
        sync.releaseShared(1);
    }

//    public void queue() {
//        sync.queue();
//    }

    public void await() {
        sync.acquireShared(1);
    }

    private static final class Sync extends MyAbstractQueuedSynchronizer {
        public Sync(int count) {
            setState(count);
        }

        /**
         * 重写tryAcquire方法await方法只能被调用一次，此处应该重写tryAcquireShared方法
         */
        @Override
        protected boolean tryAcquireShared(int arg) {
            return getState() == 0;
        }

        @Override
        protected boolean tryReleaseShared(int arg) {
            while (true) {
                int state = getState(), nextState = state - arg;
                //以防超过同步器状态数的release。
                if (state == 0) return false;
                if (compareAndSetState(state, nextState))
                    return nextState == 0;
            }
        }
    }
}


