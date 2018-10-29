[《深入拆解 Java 虚拟机》07 | JVM是如何实现反射的？](https://time.geekbang.org/column/article/12192)

---

#### 1. 反射调用的实现
方法的反射调用（Method.invoke）实际上委派给MethodAccessor来处理，MethodAccessor本身是一个接口，有两个已有的具体实现：
- 通过本地方法来实现反射调用。
- 使用委派模式：每个Method实例的第一次反射调用都会生成一个委派实现，它所委派的具体实现便是一个本地实现。

```
import java.lang.reflect.Method;

public class Test {
  public static void target(int i) {
    new Exception("#" + i).printStackTrace();
  }

  public static void main(String[] args) throws Exception {
    Class<?> klass = Class.forName("Test");
    Method method = klass.getMethod("target", int.class);
    method.invoke(null, 0);
  }
}
```
```
D:\>java Test
java.lang.Exception: #0
        at Test.target(Test.java:5)
        at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
        at sun.reflect.NativeMethodAccessorImpl.invoke(Unknown Source)
        at sun.reflect.DelegatingMethodAccessorImpl.invoke(Unknown Source)
        at java.lang.reflect.Method.invoke(Unknown Source)
        at Test.main(Test.java:11)
```
上面例子中打印了反射调用目标方法时的栈轨迹。可以发现，反射调用先是调用Method.invoke，然后进入委派实现（DelegatingMethodAccessorImpl），再然后进入本地实现（NativeMethodAccessorImpl），最后达到目标方法并执行。为什么反射调用还要采取委派实现作为中间层？
直接交给本地实现不可以么？其实Java的反射调用机制还有另一种动态生成字节码的实现（动态实现），直接使用invoke指令来调用目标方法。之所以采用委派实现，便是为了能够在本地实现和动态实现之间进行切换。

##### Inflation机制：
动态实现和本地实现相比，其运行效率要快上20倍，这是因为动态实现不需要经过Java到C++再到Java的切换，但由于生产字节码十分耗时，如果仅调用一次的话，反而是本地实现要快上3-4倍。考虑到许多反射调用仅会执行一次，Java 虚拟机设置了一个阈值 15
（可以通过 -Dsun.reflect.inflationThreshold），当某个反射调用的调用次数在 15 之下时，采用本地实现；当达到 15 时，便开始动态生成字节码，并将委派实现的委派对象切换至动态实现。反射调用的 Inflation 机制是可以通过参数（-Dsun.reflect.noInflation=true）
来关闭，这样一来在反射调用一开始便会直接生成动态实现，而不会使用委派实现或者本地实现。
```
import java.lang.reflect.Method;

public class Test {
  public static void target(int i) {
    new Exception("#" + i).printStackTrace();
  }

  public static void main(String[] args) throws Exception {
    Class<?> klass = Class.forName("Test");
    Method method = klass.getMethod("target", int.class);
    for (int i = 0; i < 20; i++) {
      method.invoke(null, i);
    }
  }
}
```
```
D:\>java Test
java.lang.Exception: #0
        at Test.target(Test.java:5)
        at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
        at sun.reflect.NativeMethodAccessorImpl.invoke(Unknown Source)
        at sun.reflect.DelegatingMethodAccessorImpl.invoke(Unknown Source)
        at java.lang.reflect.Method.invoke(Unknown Source)
        at Test.main(Test.java:12)
......
[0.160s][info][class,load] jdk.internal.reflect.GeneratedMethodAccessor1 source: __JVM_DefineClass__
java.lang.Exception: #15
        at Test.target(Test.java:5)
        at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
        at sun.reflect.NativeMethodAccessorImpl.invoke(Unknown Source)
        at sun.reflect.DelegatingMethodAccessorImpl.invoke(Unknown Source)
        at java.lang.reflect.Method.invoke(Unknown Source)
        at Test.main(Test.java:12)
java.lang.Exception: #16
        at Test.target(Test.java:5)
        at sun.reflect.GeneratedMethodAccessor1.invoke(Unknown Source)
        at sun.reflect.DelegatingMethodAccessorImpl.invoke(Unknown Source)
        at java.lang.reflect.Method.invoke(Unknown Source)
        at Test.main(Test.java:12)
......
```
可以看到，在第15次（从0开始数）反射调用时，便触发了动态实现的生成，并且从第16次反射调用开始切换至动态实现。


#### 2. 反射调用的开销
- Method.invoke变长参数导致的Object数组
- 基本类型的自动装箱、拆箱
- 反射调用可能没有被内联，或者逃逸分析没有生效

```
方法调用实际上将程序执行顺序转移到该方法所存放的内存中某个地址，并将方法体执行完后，再返回到调用该方法的地方。 这种转移操作要求在转去前要保护现场并记忆执行的地址，转回后先要恢复现场，并按原来
保存地址继续执行，也就是通常说的压栈和出栈。因此，方法调用要有一定的时间和空间方面的开销。那么对于那些方法体代码不是很大，又频繁调用的方法来说，这个时间和空间的消耗会很大。

方法内联：在程序编译时，编译器将程序中出现的方法调用表达式用方法体来直接进行替换。
```