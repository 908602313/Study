package com.project.concurrent.myThreadPool;


public interface MyRejectedExecutionHandlerInterface {
     void rejectedExecution(Runnable task, MyThreadPool pool) ;
}
