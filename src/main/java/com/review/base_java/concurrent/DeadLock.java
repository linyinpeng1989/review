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

/**
 * 测试全局锁（类锁）
 *
 * 当线程 A 持有ClassLockTest类的锁时，线程 B 依然可以访问ClassLockTest类的静态变量和非synchronized静态方法
 */
class ClassLockTest {
    private static int c = 1;

    public static synchronized void method1() throws InterruptedException {
        System.out.println("get lock, method1 start");
        Thread.sleep(2000);
        System.out.println("release lock, method1 end");
    }

    public static void method2() {
        System.out.println("start method2");
    }

    public static void main(String[] args) throws InterruptedException {
        new Thread(() -> {
            try {
                ClassLockTest.method1();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        // 等待上述线程启动
        Thread.sleep(10);
        ClassLockTest.method2();
        System.out.println("c = " + c);
    }
}

