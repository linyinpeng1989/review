[《深入拆解 Java 虚拟机》15 | Java语法糖与Java编译器](https://time.geekbang.org/column/article/13781)

---

#### 1. 自动装箱与自动拆箱
Java语言拥有8个基本类型，每个基本类型都有对应的包装（wrapper）类型。之所以需要包装类型， 是因为许多Java核心API都是面向对象的，比如Java核心类库中的容器类。
```
import java.util.ArrayList;
import java.util.List;

public class Foo {
    public int foo() {
        List<Integer> list = new ArrayList<>();
        list.add(0);
        int result = list.get(0);
        return result;
    }
}
```
```
 public int foo();
    descriptor: ()I
    flags: ACC_PUBLIC
    Code:
      stack=2, locals=3, args_size=1
         0: new           #2                  // class java/util/ArrayList
         3: dup
         4: invokespecial #3                  // Method java/util/ArrayList."<init>":()V
         7: astore_1
         8: aload_1
         9: iconst_0
        10: invokestatic  #4                  // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
        13: invokeinterface #5,  2            // InterfaceMethod java/util/List.add:(Ljava/lang/Object;)Z
        18: pop
        19: aload_1
        20: iconst_0
        21: invokeinterface #6,  2            // InterfaceMethod java/util/List.get:(I)Ljava/lang/Object;
        26: checkcast     #7                  // class java/lang/Integer
        29: invokevirtual #8                  // Method java/lang/Integer.intValue:()I
        32: istore_2
        33: iload_2
        34: ireturn
      LineNumberTable:
        line 6: 0
        line 7: 8
        line 8: 19
        line 9: 33
```
当向泛型参数为 Integer 的 ArrayList 添加 int 值时，需要进行自动装箱，调用Integer.valueOf方法将int类型的值转换为Integer类型，再存储至容器类中。
```
public static Integer valueOf(int i) {
    if (i >= IntegerCache.low && i <= IntegerCache.high)
        return IntegerCache.cache[i + (-IntegerCache.low)];
    return new Integer(i);
}
```
Integer.valueOf方法中，当请求的int值在某个范围内时，返回缓存的Integer对象；当请求的int值在范围之外时，会新建一个Integer对象。

当从泛型参数为 Integer 的 List 取出元素时，实际上也是返回 Integer 对象。如果应用程序期待的是一个 int 值，那么就会发生自动拆箱，调用Integer.intValue方法将Integer对象转换为int值。


#### 2. 泛型与类型擦除
```
    ......
    10: invokestatic  #4                  // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
    13: invokeinterface #5,  2            // InterfaceMethod java/util/List.add:(Ljava/lang/Object;)Z
    18: pop
    19: aload_1
    20: iconst_0
    21: invokeinterface #6,  2            // InterfaceMethod java/util/List.get:(I)Ljava/lang/Object;
    26: checkcast     #7                  // class java/lang/Integer
    29: invokevirtual #8                  // Method java/lang/Integer.intValue:()I
    ......
```
在上述字节码中可以看到，往List中添加元素的add方法接收的参数类型是Object，而从List中获取元素的get方法的返回类型为Object，且需要进行强制类型转换才能获得目标类型的结果。
之所以会出现这种情况，是因为Java泛型的类型擦除，即Java程序里的泛型信息，在Java虚拟机里全部消失了。

Java 编译器将选取该泛型所能指代的所有类中层次最高的那个，作为替换泛型的类。对于限定了继承类的泛型参数，经过类型擦除后，所有的泛型参数都将变成所限定的继承类；对于没有限定泛型参数的，经过类型擦除后，所有泛型参数都将变成Object类。

#### 3. 桥接方法
[Java中bridge方法探秘](http://chaser520.iteye.com/admin/blogs/2432696)

#### 4. 其他语法糖
- try-catch-resources
- foreach循环：foreach 循环允许 Java 程序在 for 循环里遍历数组或者 Iterable 对象。
    - 对于数组来说，foreach 循环将从 0 开始逐一访问数组中的元素，直至数组的末尾。
    ```
    public void foo(int[] array) {
      for (int item : array) {
      }
    }
    // 等同于
    public void bar(int[] array) {
      int[] myArray = array;
      int length = myArray.length;
      for (int i = 0; i < length; i++) {
        int item = myArray[i];
      }
    }
    ```
    - 对于 Iterable 对象来说，foreach 循环将调用其 iterator 方法，并且用它的 hasNext 以及 next 方法来遍历该 Iterable 对象中的元素。
    ```
    public void foo(ArrayList<Integer> list) {
      for (Integer item : list) {
      }
    }
    // 等同于
    public void bar(ArrayList<Integer> list) {
      Iterator<Integer> iterator = list.iterator();
      while (iterator.hasNext()) {
        Integer item = iterator.next();
      }
    }
    ```
    - 字符串switch：由于每个 case 所截获的字符串都是常量值，Java 编译器会将原来的字符串 switch 转换为 int 值 switch，比较所输入的字符串的哈希值。由于字符串哈希值很容易发生碰撞，我们还需要用 String.equals 逐个比较相同哈希值的字符串。