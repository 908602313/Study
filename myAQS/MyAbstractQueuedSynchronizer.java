package com.project.concurrent.myAQS;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public abstract class MyAbstractQueuedSynchronizer implements MyInterfaceQueuedSynchronizer {
    private volatile Thread exclusiveHeldThread;
    private volatile int state;

    static final long SPIN_TIME = 1000L;

    private volatile Node head = new Node();
    private volatile Node tail = head;
    private static final VarHandle STATE;
    private static final VarHandle TAIL;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            STATE = lookup.findVarHandle(MyAbstractQueuedSynchronizer.class, "state", int.class);
            TAIL = lookup.findVarHandle(MyAbstractQueuedSynchronizer.class, "tail", Node.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public class MyCondition implements MyConditionInterface {
        private final Node first = new Node();
        private volatile Node last = first;

        @Override
        public final void await() throws InterruptedException {
            if (Thread.interrupted()) throw new InterruptedException();
            Thread thread = Thread.currentThread();
            checkHeldExclusive(thread);
            Node node = new Node(Node.CONDITION, thread), toClear = null;
            enConditionQueue(node);
            int saved = fullyRelease();
            boolean interrupted = false;
            while (node.waitState == Node.CONDITION) {
                LockSupport.park(this);
                if (Thread.interrupted()) {
                    interrupted = true;
                    if (Node.WAIT_STATE.compareAndSet(node, Node.CONDITION, Node.CANCELLED)) {
                        node.thread = null;
                        toClear = node;
                        node = new Node(Node.EXCLUSIVE, thread);
                        enSyncQueue(node);
                    }
                    break;
                }
            }
            doAcquire(node, saved);
            if (toClear != null && toClear.waitState != Node.CLEARED)
                unlinkCanceledWaiters();
            if (interrupted)
                throw new InterruptedException();
        }

        @Override
        public boolean await(long time, TimeUnit unit) throws InterruptedException {
            if (time <= 0) throw new IllegalArgumentException("time<=0");
            return await(unit.toNanos(time));
        }

        @Override
        public boolean await(long nanoTime) throws InterruptedException {
            if (Thread.interrupted()) throw new InterruptedException();
            Thread thread = Thread.currentThread();
            checkHeldExclusive(thread);
            if (nanoTime <= 0) throw new IllegalArgumentException("nanoTime<=0");
            Node node = new Node(Node.CONDITION, thread), toClear = null;
            enConditionQueue(node);
            int saved = fullyRelease();
            long remaining = nanoTime, deadLine = System.nanoTime() + remaining;
            boolean interrupted = false;
            while (node.waitState == Node.CONDITION) {
                if (remaining <= 0 || interrupted) {
                    if (Node.WAIT_STATE.compareAndSet(node, Node.CONDITION, Node.CANCELLED)) {
                        node.thread = null;
                        toClear = node;
                        node = new Node(Node.EXCLUSIVE, thread);
                        enSyncQueue(node);
                    }
                    break;
                }
                if (remaining > SPIN_TIME)
                    LockSupport.parkNanos(this, remaining);
                interrupted = Thread.interrupted();
                remaining = deadLine - System.nanoTime();
            }
            doAcquire(node, saved);
            if (toClear != null && toClear.waitState != Node.CLEARED)
                unlinkCanceledWaiters();
            if (interrupted)
                throw new InterruptedException();
            return remaining < 0;
        }

        @Override
        public final void signal() {
            checkHeldExclusive(Thread.currentThread());
            while (true) {
                Node firstNext = first.next;
                if (firstNext == null)
                    return;
                if (firstNext.waitState == Node.CANCELLED)
                    unlinkCanceledWaiters();
                if (Node.WAIT_STATE.compareAndSet(firstNext, Node.CONDITION, Node.EXCLUSIVE)) {
                    first.next = firstNext.next;
                    if (last == firstNext)
                        last = first;
                    enSyncQueue(firstNext);
                    firstNext.waitState = Node.EXCLUSIVE;
                    LockSupport.unpark(firstNext.thread);
                    return;
                }
            }
        }

        @Override
        public final void signalAll() {
            checkHeldExclusive(Thread.currentThread());
            Node firstNext = first.next;
            if (firstNext == null) return;
            unlinkCanceledWaiters();
            enSyncQueue(firstNext, last);
            for (Node p = firstNext; p != null; p = p.next)
                Node.WAIT_STATE.compareAndSet(p, Node.CONDITION, Node.EXCLUSIVE);
            first.next = null;
            last = first;
            unparkFirstWaiter();
        }

        final void enConditionQueue(Node node) {
            last.next = node;
            last = node;
        }


        final void unlinkCanceledWaiters() {
            Node dummyHead = new Node(), p1 = first.next, p2 = dummyHead;
            while (p1 != null) {
                if (p1.waitState == Node.CONDITION)
                    p2 = p2.next = p1;
                else
                    p1.waitState = Node.CLEARED;
                p1 = p1.next;
            }
            p2.next = null;
            if (p2 == dummyHead) {
                first.next = null;
                last = first;
            } else {
                first.next = dummyHead.next;
                last = p2;
            }
        }
    }

    static class Node {
        private static final int CLEARED = -2;
        private static final int CANCELLED = -1;
        private static final int EXCLUSIVE = 0;
        private static final int SHARED = 1;
        private static final int CONDITION = 2;


        public Node(int waitState, Thread thread) {
            this.waitState = waitState;
            this.thread = thread;
        }


        public Node() {
        }

        public static boolean isAlive(int waitState) {
            return waitState >= 0;
        }

        private volatile int waitState;
        private volatile Thread thread;
        private volatile Node next;


        private static final VarHandle WAIT_STATE;

        static {
            try {
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                WAIT_STATE = lookup.findVarHandle(Node.class, "waitState", int.class);
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

    }

    @Override
    public final void acquire(int arg) {
        if (!tryAcquire(arg)) {
            Node node = new Node(Node.EXCLUSIVE, Thread.currentThread());
            enSyncQueue(node);
            doAcquire(node, arg);
        }
//        unparkFirstWaiter();
    }

    public final void acquireInterruptibly(int arg)
            throws InterruptedException {
        if (Thread.interrupted()) throw new InterruptedException();
        if (!tryAcquire(arg)) {
            Node node = new Node(Node.EXCLUSIVE, Thread.currentThread());
            enSyncQueue(node);
            doAcquireInterruptibly(node, arg);
        }
    }

    final void doAcquireInterruptibly(Node node, int arg) throws InterruptedException {
        while (true) {
            Node headNext = head.next;
            if (Thread.interrupted()) {
                node.waitState = Node.CANCELLED;
                node.thread = null;
                if (headNext == node)
                    unlinkCancelledNodesInSyncQueue();
                throw new InterruptedException();
            }
            if (node == headNext && tryAcquire(arg)) {
                setHead(node);
                return;
            }
            LockSupport.park(this);

        }
    }


    @Override
    public final void acquireShared(int arg) {
        if (!tryAcquireShared(arg)) {
            Node node = new Node(Node.SHARED, Thread.currentThread());
            enSyncQueue(node);
            doAcquireShared(node, arg);
        }
        if (firstWaiterIsShared())
            unparkFirstWaiter();
    }

    public final void acquireSharedInterruptibly(int arg)
            throws InterruptedException {
        if (Thread.interrupted()) throw new InterruptedException();
        if (!tryAcquireShared(arg)) {
            Node node = new Node(Node.SHARED, Thread.currentThread());
            enSyncQueue(node);
            doAcquireSharedInterruptibly(node, arg);
        }
        if (firstWaiterIsShared())
            unparkFirstWaiter();
    }

    protected final void doAcquireSharedInterruptibly(Node node, int arg)
            throws InterruptedException {
        while (true) {
            Node headNext = head.next;
            if (Thread.interrupted()) {
                node.waitState = Node.CANCELLED;
                node.thread = null;
                if (headNext == node)
                    unlinkCancelledNodesInSyncQueue();
                throw new InterruptedException();
            }
            if (node == headNext && tryAcquireShared(arg)) {
                setHead(node);
                return;
            }
            LockSupport.park(this);
        }
    }


    protected final boolean firstWaiterIsExclusive() {
        Node headNext = head.next;
        return headNext != null && headNext.waitState == Node.EXCLUSIVE;
    }

    protected final boolean firstWaiterIsShared() {
        Node headNext = head.next;
            return headNext != null && headNext.waitState == Node.SHARED;
    }

    protected final void setExclusiveHeldThread(Thread thread) {
        exclusiveHeldThread = thread;
    }

    protected final void checkHeldExclusive(Thread thread) {
        if (exclusiveHeldThread != thread)
            throw new IllegalMonitorStateException("当前线程未独占同步器");
    }

    public final boolean heldExclusive(Thread thread) {
        return exclusiveHeldThread == thread;
    }

    @Override
    public final boolean release(int arg) {
        checkHeldExclusive(Thread.currentThread());
        if (tryRelease(arg)) {
            unparkFirstWaiter();
            return true;
        }
        return false;
    }

    public void showSyncQueue() {
        System.out.print("queue:");
        Node node = head;
        while (node != null) {
            System.out.print(node.thread + ",");
            node = node.next;
        }
        System.out.println();
    }

    @Override
    public final boolean releaseShared(int arg) {
        if (tryReleaseShared(arg)) {
            unparkFirstWaiter();
            return true;
        }
        return false;
    }

    protected boolean tryAcquire(int arg) {
        throw new UnsupportedOperationException();
    }

    final void doAcquire(Node node, int arg) {
        while (true) {
            Node headNext = head.next;
            if (node == headNext && tryAcquire(arg)) {
                setHead(node);
                return;
            }
//            showSyncQueue();
            LockSupport.park(this);
        }
    }

    final void setHead(Node node) {
        head = node;
        node.thread = null;
    }

    final void unparkFirstWaiter() {
//        Node head = this.head;
//        if (head == null) return;
        Node headNext = head.next;
        if (headNext != null) {
            int state = headNext.waitState;
            if (Node.isAlive(state))
                LockSupport.unpark(headNext.thread);
            else if (state == Node.CANCELLED)
                unlinkCancelledNodesInSyncQueue();
        }
    }

    final boolean unlinkCancelledNodesInSyncQueue() {
        Node head = this.head, headNext;
        if ((headNext = head.next) == null
                || headNext.waitState != Node.CANCELLED
                || Node.WAIT_STATE.compareAndSet(headNext, Node.CANCELLED, Node.CLEARED))
            return false;
        tail = head;
        Node dummyHead = new Node(), p1 = headNext.next, p2 = dummyHead;
        while (p1 != null) {
            if (p1.waitState != Node.CANCELLED)
                p2 = p2.next = p1;
            p1 = p1.next;
        }
        if (p2 != dummyHead) {
            p2.next = null;
            enSyncQueue(dummyHead.next, p2);
        }
        unparkFirstWaiter();
        return true;
    }


    protected boolean tryAcquireShared(int arg) {
        throw new UnsupportedOperationException();
    }

    final void doAcquireShared(Node node, int arg) {
        while (true) {
            Node headNext = head.next;
            if (node == headNext && tryAcquireShared(arg)) {
                setHead(node);
                return;
            }
            LockSupport.park(this);
        }
    }

    protected boolean tryRelease(int arg) {
        throw new UnsupportedOperationException();
    }

    protected boolean tryReleaseShared(int arg) {
        throw new UnsupportedOperationException();
    }


    final int fullyRelease() {

        int saved = state;
        if (release(saved))
            return saved;
        throw new IllegalMonitorStateException();
    }

    final void enSyncQueue(Node node) {
        enSyncQueue(node, node);
    }

    final void enSyncQueue(Node first, Node last) {
        while (true) {
            Node tail = this.tail;
            if (TAIL.compareAndSet(this, tail, last)) {
                tail.next = first;
                return;
            }
        }
    }

    protected final boolean isFirstInSyncQueue() {
        Node headNext = head.next;
        boolean ans = headNext != null && headNext.thread == Thread.currentThread();
        if (!ans) {
            synchronized (this) {
                if (headNext != null)
                    System.out.println(headNext.thread.getName());
                System.out.println(Thread.currentThread().getName());
                System.out.println();
            }
        }
        return ans;
    }

//    final void pushSyncQueue(Node first, Node last) {
//        while (true) {
//            Node headNext = head.next;
//            last.next = headNext;
//            if (Node.NEXT.compareAndSet(head, headNext, first)) {
//                if (headNext == null) tail = last;
//                return;
//            }
//        }
//    }


    protected final int getState() {
        return state;
    }


    protected final void setState(int newState) {
        state = newState;
    }

    protected final boolean compareAndSetState(int expect, int update) {
        return STATE.compareAndSet(this, expect, update);
    }
}
