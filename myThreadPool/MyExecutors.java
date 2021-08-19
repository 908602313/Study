package com.project.concurrent.myThreadPool;


import java.util.concurrent.TimeUnit;

public class MyExecutors {

    public static MyThreadPool newFixedThreadPool(int n){
        return new MyThreadPool(n,n,0, TimeUnit.SECONDS);
    }
}
