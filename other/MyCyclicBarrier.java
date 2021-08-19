package com.project.concurrent.other;

import com.project.concurrent.myAQS.MyAbstractQueuedSynchronizer;


public class MyCyclicBarrier {
    private final Sync sync;

    public MyCyclicBarrier(int parties) {
        if (parties <= 0 || parties > Sync.MAX_LEAVE_COUNT)
            throw new IllegalArgumentException("parties<=0||parties>" + Sync.MAX_LEAVE_COUNT);
        this.sync = new Sync(parties);
    }

    private static final class Sync extends MyAbstractQueuedSynchronizer {
        private static final int SHIFT = 17;
        private static final int MAX_ARRIVAL_COUNT = (1 << SHIFT) - 1;
        private static final int MAX_LEAVE_COUNT = (1 << (Integer.SIZE - SHIFT)) - 1;
        private final int parties;
        private final ThreadLocal<Integer> order = new ThreadLocal<>();

        public Sync(int parties) {
            this.parties = parties;
        }

        @Override
        protected boolean tryAcquireShared(int arg) {

            while (true) {
                int state = getState();
                int arrivalCount = getArrivalCount(state);
                int leaveCount = getLeaveCount(state);
                if (leaveCount >= parties) {
//                    System.out.println("leaveCount >= parties");
                    compareAndSetState(state, state - parties - (parties << SHIFT));
                    continue;
                }
                if (arrivalCount / parties <= leaveCount / parties) {
//                    System.out.println("arrivalCount / parties:" + arrivalCount / parties + ",leaveCount / parties:" + leaveCount / parties);
                    return false;
                }
                if (compareAndSetState(state, state + (arg << SHIFT))) {
                    order.set(leaveCount % parties + 1);
//                    System.out.println("nextState:" + state + (arg << SHIFT));
                    return true;
                }
            }
        }

        @Override
        protected boolean tryReleaseShared(int arg) {
            int state = getState();
            return getArrivalCount(state) / parties > getLeaveCount(state) / parties;
        }

        private int getArrivalCount(int state) {
            return state & MAX_ARRIVAL_COUNT;
        }

        private int getLeaveCount(int state) {
            return state >> SHIFT;
        }

        private int getAndAddArrivalCount(int arg) {
            while (true) {
                int state = getState(), nextState = state + arg;
                if (compareAndSetState(state, nextState)) {
//                    System.out.println(state);
                    return state;
                }
            }
        }

    }

    public int await() throws InterruptedException {
        int state = sync.getAndAddArrivalCount(1);
        sync.releaseShared(1);
        sync.acquireSharedInterruptibly(1);
        return sync.order.get();
    }
}
