package com.review.base_java.jvm_.practice.demo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 该实战例子来自于微信公众号文章
 * @see <a href="https://mp.weixin.qq.com/s?__biz=MzI5NTYwNDQxNA==&mid=2247484418&idx=1&sn=883b43106a4af5ce63772423834fb457&chksm=ec505dd3db27d4c5e479a686b4bf303c2e2302ec335db3ff5145d4b29d4b245982fbd2be0a7c&mpshare=1&scene=1&srcid=1108rAZdmaaLO6OW2vaQJCaA#rd">又是一个程序员粗心的代码引起频繁FullGC的案例</a>
 * @desc CPU飙高并且频繁FullGC
 */
public class FullGCDemo {
    /**
     * 线程执行器
     */
    private static ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(50, new ThreadPoolExecutor.DiscardOldestPolicy());

    public static void main(String[] args) throws InterruptedException {
        executor.setMaximumPoolSize(50);

        // 模拟xxl-job 100ms调用一次
        for (int index = 0; index < Integer.MAX_VALUE; index++) {
            buildBar();
            Thread.sleep(100);
        }
    }

    private static void buildBar() {
        List<FutureContract> futureContracts = getAllFutureConstract();
        futureContracts.forEach(futureContract -> {
            executor.scheduleWithFixedDelay(() -> {
                try {
                    doFutureContract(futureContract);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 2, 3, TimeUnit.SECONDS);
        });
    }

    private static void doFutureContract(FutureContract futureContract) {
        System.out.println(futureContract.toString());
    }


    private static List<FutureContract> getAllFutureConstract() {
        List<FutureContract> futureContracts = new ArrayList<>();
        // 创建100个FutureContract对象
        for (int index = 0; index < 100; index++) {
            FutureContract futureContract = new FutureContract(new BigDecimal("1.0"), new BigDecimal("1.0"),new BigDecimal("1.0"),new BigDecimal("1.0"),
                    new BigDecimal("1.0"),new BigDecimal("1.0"),new BigDecimal("1.0"),new BigDecimal("1.0"),new BigDecimal("1.0"),new BigDecimal("1.0"),
                    new BigDecimal("1.0"),new BigDecimal("1.0"),new BigDecimal("1.0"),new BigDecimal("1.0"),new BigDecimal("1.0"),new BigDecimal("1.0"),
                    new Long(1L), new Long(1L), new Long(1L),
                    new String("123"), new String("456"),new String("789"),
                    new Integer(1), new Integer(1),new Integer(1),new Integer(1),
                    new Date(), new Date());
            futureContracts.add(futureContract);
        }
        return futureContracts;
    }

    private static class FutureContract {
        /** 16个BigDecimal类型属性 */
        private BigDecimal bd1;
        private BigDecimal bd2;
        private BigDecimal bd3;
        private BigDecimal bd4;
        private BigDecimal bd5;
        private BigDecimal bd6;
        private BigDecimal bd7;
        private BigDecimal bd8;
        private BigDecimal bd9;
        private BigDecimal bd10;
        private BigDecimal bd11;
        private BigDecimal bd12;
        private BigDecimal bd13;
        private BigDecimal bd14;
        private BigDecimal bd15;
        private BigDecimal bd16;
        /** 3个Long类型属性 */
        private Long l1;
        private Long l2;
        private Long l3;
        /** 3个String类型属性 */
        private String s1;
        private String s2;
        private String s3;
        /** 四个Integer类型属性 */
        private Integer i1;
        private Integer i2;
        private Integer i3;
        private Integer i4;
        /** 2个Date类型属性 */
        private Date date1;
        private Date date2;

        public FutureContract(BigDecimal bd1, BigDecimal bd2, BigDecimal bd3, BigDecimal bd4, BigDecimal bd5, BigDecimal bd6, BigDecimal bd7,
                              BigDecimal bd8, BigDecimal bd9, BigDecimal bd10, BigDecimal bd11, BigDecimal bd12, BigDecimal bd13, BigDecimal bd14,
                              BigDecimal bd15, BigDecimal bd16, Long l1, Long l2, Long l3, String s1, String s2, String s3, Integer i1, Integer i2,
                              Integer i3, Integer i4, Date date1, Date date2) {
            this.bd1 = bd1;
            this.bd2 = bd2;
            this.bd3 = bd3;
            this.bd4 = bd4;
            this.bd5 = bd5;
            this.bd6 = bd6;
            this.bd7 = bd7;
            this.bd8 = bd8;
            this.bd9 = bd9;
            this.bd10 = bd10;
            this.bd11 = bd11;
            this.bd12 = bd12;
            this.bd13 = bd13;
            this.bd14 = bd14;
            this.bd15 = bd15;
            this.bd16 = bd16;
            this.l1 = l1;
            this.l2 = l2;
            this.l3 = l3;
            this.s1 = s1;
            this.s2 = s2;
            this.s3 = s3;
            this.i1 = i1;
            this.i2 = i2;
            this.i3 = i3;
            this.i4 = i4;
            this.date1 = date1;
            this.date2 = date2;
        }

        @Override
        public String toString() {
            return "FutureContract{" +
                    "bd1=" + bd1 +
                    ", bd2=" + bd2 +
                    ", bd3=" + bd3 +
                    ", bd4=" + bd4 +
                    ", bd5=" + bd5 +
                    ", bd6=" + bd6 +
                    ", bd7=" + bd7 +
                    ", bd8=" + bd8 +
                    ", bd9=" + bd9 +
                    ", bd10=" + bd10 +
                    ", bd11=" + bd11 +
                    ", bd12=" + bd12 +
                    ", bd13=" + bd13 +
                    ", bd14=" + bd14 +
                    ", bd15=" + bd15 +
                    ", bd16=" + bd16 +
                    ", l1=" + l1 +
                    ", l2=" + l2 +
                    ", l3=" + l3 +
                    ", s1='" + s1 + '\'' +
                    ", s2='" + s2 + '\'' +
                    ", s3='" + s3 + '\'' +
                    ", i1=" + i1 +
                    ", i2=" + i2 +
                    ", i3=" + i3 +
                    ", i4=" + i4 +
                    ", date1=" + date1 +
                    ", date2=" + date2 +
                    '}';
        }
    }

}
