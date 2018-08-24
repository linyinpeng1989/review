package com.review.base_java.other;

/**
 * @author: Yinpeng.Lin
 * @desc: 代码执行顺序
 * @date: Created in 2018/8/24 10:31
 *
 * 父类静态域或静态代码块（依赖于代码顺序）、子类静态域或静态代码块（依赖于代码顺序）、父类语句块、父类构造器、子类语句块、子类构造器
 */
public class OrderTest extends OrderSuper {

    public static String subStr = "def";
    static {
        System.out.println("子类静态语句块");
        subStr = "abc";
    }

    {
        System.out.println("子类普通语句块");
    }

    public OrderTest() {
        subStr = "abc";
        System.out.println("OrderTest构造方法");
    }

    public String getStr() {
        return subStr;
    }

    public static void main(String[] args) {
        OrderTest test = new OrderTest();
        System.out.println(test.getStr());
    }

}

class OrderSuper {
    public static String parentStr = "def";
    static {
        System.out.println("父类静态语句块");
        parentStr = "abc";
    }

    {
        System.out.println("父类普通语句块");
    }

    public OrderSuper() {
        parentStr = "abc";
        System.out.println("OrderSuper构造方法");
    }
}
