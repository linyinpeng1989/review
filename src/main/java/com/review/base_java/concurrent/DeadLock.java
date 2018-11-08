package com.review.base_java.concurrent;

/**
 * @author: Yinpeng.Lin
 * @desc:
 * @date: Created in 2018/8/22 14:10
 */
public class DeadLock {
    private static int c = 1;

    private static Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
            c = 2;
            System.out.println(c);
            System.out.println("执行test-thread线程");
        }
    }, "test-thread");

    static {
        thread.start();
        try {
            // join方法作用：阻塞等待thread线程执行完毕
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.out.println("OK");
    }
}