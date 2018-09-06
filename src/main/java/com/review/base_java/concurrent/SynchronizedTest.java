package com.review.base_java.concurrent;

/**
 * @author: Yinpeng.Lin
 * @desc:
 * @date: Created in 2018/9/6 21:12
 */
public class SynchronizedTest implements Runnable {
    int b = 100;

    synchronized void m1() throws InterruptedException {
        b = 1000;
        Thread.sleep(500);  // 6
        System.out.println("b = " + b);
    }

    synchronized void m2() throws InterruptedException {
        Thread.sleep(250);  // 5
        b = 2000;
    }

    @Override
    public void run() {
        try {
            m1();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        SynchronizedTest synchronizedTest = new SynchronizedTest();
        Thread t = new Thread(synchronizedTest);    // 1
        t.start();  // 2

        // 模拟2步骤先获得锁
        // Thread.sleep(5);

        synchronizedTest.m2();  // 3
        System.out.println("main thread b = " + synchronizedTest.b);    // 4
    }
}
