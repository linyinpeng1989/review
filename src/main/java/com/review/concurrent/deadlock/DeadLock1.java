package com.review.concurrent.deadlock;

/**
 * @author: Yinpeng.Lin
 * @desc:
 * @date: Created in 2018/8/22 14:10
 */
public class DeadLock1 {
    private static int c = 1;
    /**
     * lambda表达式创建Runnable匿名对象
     */
    private static Thread thread = new Thread(() -> {
        c = 2;
        System.out.println(c);
    });

    static {
        thread.start();
        try {
            /*
             * join()方法的作用：放弃当前线程的执行，并返回对应的线程继续执行，等对应的线程执行完毕后，再返回当前线程继续执行。
             *
             * 程序在<clinit>线程中调用thread线程的join方法，则<clinit>线程放弃cpu控制权，并返回thread线程继续执行直到线程thread执行完毕
             * 所以结果是thread线程执行完后，才到<clinit>线程执行，相当于在<clinit>线程中同步thread线程，thread执行完了，<clinit>线程才有执行的机会
             */
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.out.println("OK");
    }
}
