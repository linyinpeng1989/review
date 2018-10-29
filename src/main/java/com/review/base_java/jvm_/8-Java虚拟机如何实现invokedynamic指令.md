[《深入拆解 Java 虚拟机》08 | JVM是怎么实现invokedynamic的？（上）](https://time.geekbang.org/column/article/12564)

[《深入拆解 Java 虚拟机》08 | JVM是怎么实现invokedynamic的？（下）](https://time.geekbang.org/column/article/12574)
---

在Java中，方法调用会被编译为invokestatic、invokespecial、invokevirtual 以及 invokeinterface 四种指令。这些指令与包含目标方法类名、方法名以及方法描述符的符号引用捆绑。在实际运行之前，Java虚拟机将
根据这个符号引用链接到具体的目标方法。在这四种调用指令中，Java虚拟机明确要求方法调用需要提供目标方法的类名。

那么如何用同一种方式调用不同类的同一方法呢？为了解决这个问题，Java 7中引入了一条新的指令invokedynamic。该指令的调用机制抽象出调用点这个概念， 并允许应用程序将调用点链接至任意符合条件的方法上。
作为invokedynamic的准备工作，Java 7引入了更加底层、更加灵活的方法抽象：方法句柄（MethodHandle）。

#### 1.方法句柄的概念
方法句柄是一个强类型的、能够被直接执行的引用，它可以指向常规的静态方法或者实例方法，也可以指向构造器或者字段。当指向字段时，方法句柄实际上指向包含字段访问字节码的虚构方法，语义上等价于目标字段的
getter或者setter方法。

方法句柄的类型（MethodType）是由所指向方法的参数类型以及返回类型组成的，它是用来确认方法句柄是否适配的唯一关键。当使用方法句柄时，我们其实并不关心方法句柄所指向方法的类名或者方法名。

方法句柄的创建是通过MethodHandles.Lookup类来完成的，它提供了多个API，既可以使用反射API中的Method进行查找，也可以根据类、方法名、方法句柄类型进行查找。当使用后者时，需要区分具体的调用类型，比如对
于用invokestatic调用的静态方法，需要使用Lookup.findStatic方法；对于用invokevirtual调用的实例方法以及用invokeinterface调用的接口方法，需要使用Lookup.findVirtual方法；对于用invokespecial调用的私有
方法以及构造器等，需要使用Lookup.findSpecial方法。对于原本用 invokevirtual 调用的方法句柄，它也会采用动态绑定；而对于原本用 invkespecial 调用的方法句柄，它会采用静态绑定。
```
class Foo {
  private static void bar(Object o) {
    ..
  }
  public static Lookup lookup() {
    return MethodHandles.lookup();
  }
}

MethodHandles.Lookup lookup = Foo.lookup(); // 具备 Foo 类的访问权限

// 使用反射API中的Method进行查找
Method m = Foo.class.getDeclaredMethod("bar", Object.class);
MethodHandle mh0 = lookup.unreflect(m);

// 根据类、方法名、方法句柄类型进行查找
MethodType t = MethodType.methodType(void.class, Object.class);
MethodHandle mh1 = lookup.findStatic(Foo.class, "bar", t);
```
方法句柄也有权限问题，但它与反射API不同，其权限检查是在句柄的创建阶段完成的。在实际调用过程中，Java 虚拟机并不会检查方法句柄的权限。如果该句柄被多次调用的话，那么与反射调用相比，它将省下重复
权限检查的开销。需要注意的是，方法句柄的访问权限不取决于方法句柄的创建位置，而是取决于 Lookup 对象的创建位置。举个例子，对于一个私有字段，如果 Lookup 对象是在私有字段所在类中获取的，那么这个
Lookup 对象便拥有对该私有字段的访问权限，即使是在所在类的外边，也能够通过该 Lookup 对象创建该私有字段的 getter 或者 setter。

#### 2.方法句柄的操作
方法句柄的调用可以分为两种：
- invokeExact：需要严格匹配参数类型，即要求方法描述符严格匹配。比如方法参数类型为Object，而传入String作为实际参数，则方法句柄的调用会在运行时抛出方法类型不匹配的异常。
- invoke：自动适配参数类型。它会调用MethodHandle.asType方法生成一个适配器方法句柄，对于传入的参数进行适配，然后再调用原方法句柄。调用原方法句柄的返回值同样也会先进行适配，然后再返回给调用者。

方法句柄还支持增删改参数的操作，通过生成另一个方法句柄来实现：
- 改操作：即调用MethodHandle.asType方法生成一个适配器方法句柄。
- 删操作：即调用MethodHandles.dropArguments方法，将传入的部分参数就地抛弃，再调用另一个方法句柄。
- 增操作：即调用MethodHandle.bindTo方法，往传入的参数中插入额外的参数，在调用另一个方法句柄。

#### 3.方法句柄的实现
invokeExact和invoke都是签名多态性的方法，即Java 编译器会根据所传入参数的声明类型来生成方法描述符，而不是采用目标方法所声明的描述符。编译如下代码进行分析：
```
import java.lang.invoke.*;

public class Foo {
  // 通过新建异常实例打印栈轨迹
  public static void bar(Object o) {
    new Exception().printStackTrace();
  }

  public static void main(String[] args) throws Throwable {
    MethodHandles.Lookup l = MethodHandles.lookup();
    MethodType t = MethodType.methodType(void.class, Object.class);
    MethodHandle mh = l.findStatic(Foo.class, "bar", t);
    mh.invokeExact(new Object());
  }
}
```
```
//  -XX:+ShowHiddenFrames这个参数用于打印被Java虚拟机隐藏的栈信息
// -Djava.lang.invoke.MethodHandle.DUMP_CLASS_FILES=true 将自动生成的类导出成class文件
D:\>java -XX:+UnlockDiagnosticVMOptions -XX:+ShowHiddenFrames -Djava.lang.invoke.MethodHandle.DUMP_CLASS_FILES=true Foo
Dumping class files to DUMP_CLASS_FILES/...
dump: DUMP_CLASS_FILES\java\lang\invoke\LambdaForm$DMH000.class
dump: DUMP_CLASS_FILES\java\lang\invoke\LambdaForm$DMH001.class
......
dump: DUMP_CLASS_FILES\java\lang\invoke\LambdaForm$MH012.class
java.lang.Exception
        at Foo.bar(Foo.java:5)
        at java.lang.invoke.LambdaForm$DMH001/2101973421.invokeStatic_001_L_V(LambdaForm$DMH001:1000010)
        at java.lang.invoke.LambdaForm$MH012/1878246837.invokeExact_000_MT(LambdaForm$MH012:1000016)
        at Foo.main(Foo.java:12)
```
可以发现，当调用方法句柄的invokeExact方法时，Java 虚拟机会对 invokeExact 调用做特殊处理，调用至一个共享的、与方法句柄类型相关的特殊适配器中（LambdaForm$MH012）。
```
D:\>javap -v DUMP_CLASS_FILES\java\lang\invoke\LambdaForm$MH012.class
Classfile /D:/DUMP_CLASS_FILES/java/lang/invoke/LambdaForm$MH012.class
  Last modified 2018-10-27; size 862 bytes
  MD5 checksum 7d17459404b67e053254f8b495d3bdc6
  Compiled from "LambdaForm$MH012"
final class java.lang.invoke.LambdaForm$MH012
  minor version: 0
  major version: 52
  flags: ACC_FINAL, ACC_SUPER
Constant pool:
   #1 = Utf8               java/lang/invoke/LambdaForm$MH012
   #2 = Class              #1             // java/lang/invoke/LambdaForm$MH012
......
{
  static void invokeExact_000_MT(java.lang.Object, java.lang.Object, java.lang.Object);
    descriptor: (Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
    flags: ACC_STATIC
    Code:
      stack=2, locals=3, args_size=3
         0: aload_0
         1: aload_2
         2: invokestatic  #16                 // Method java/lang/invoke/Invokers.checkExactType:(Ljava/lang/Object;Ljava/lang/Object;)V
         5: aload_0
         6: invokestatic  #20                 // Method java/lang/invoke/Invokers.checkCustomized:(Ljava/lang/Object;)V
         9: aload_0
        10: checkcast     #22                 // class java/lang/invoke/MethodHandle
        13: dup
        14: astore_0
        15: aload_1
        16: invokevirtual #25                 // Method java/lang/invoke/MethodHandle.invokeBasic:(Ljava/lang/Object;)V
        19: return
    RuntimeVisibleAnnotations:
      0: #8()
      1: #9()
      2: #10()

  static void dummy();
    descriptor: ()V
    flags: ACC_STATIC
    Code:
      stack=1, locals=0, args_size=0
         0: ldc           #29                 // String invokeExact_000_MT=Lambda(a0:L,a1:L,a2:L)=>{\n    t3:V=Invokers.checkExactType(a0:L,a2:L);\n    t4:V=Invokers.checkCustomized(a0:L);\n    t5:V=MethodHandle.invokeBasic(a0:L,a1:L);void}
         2: pop
         3: return
}
SourceFile: "LambdaForm$MH012"
```
可以看到在这个适配器中，会调用 Invokers.checkExactType 方法来检查参数类型，然后调用 Invokers.checkCustomized 方法。后者会在方法句柄的执行次数超过一个阈值时进行优化
（对应参数 -Djava.lang.invoke.MethodHandle.CUSTOMIZE_THRESHOLD，默认值为 127）。最后，它会调用方法句柄的 invokeBasic 方法。Java 虚拟机同样会对 invokeBasic 调用做特殊处理，
这将调用至方法句柄本身所持有的适配器中，这个适配器同样是一个 LambdaForm（LambdaForm$DMH001）。
```
D:\>javap -v DUMP_CLASS_FILES\java\lang\invoke\LambdaForm$DMH001.class
Classfile /D:/DUMP_CLASS_FILES/java/lang/invoke/LambdaForm$DMH001.class
  Last modified 2018-10-27; size 860 bytes
  MD5 checksum 33913c0d289403cf6073d767a1a5cd92
  Compiled from "LambdaForm$DMH001"
final class java.lang.invoke.LambdaForm$DMH001
  minor version: 0
  major version: 52
  flags: ACC_FINAL, ACC_SUPER
Constant pool:
   #1 = Utf8               java/lang/invoke/LambdaForm$DMH001
   #2 = Class              #1             // java/lang/invoke/LambdaForm$DMH001
......
{
  static void invokeStatic_001_L_V(java.lang.Object, java.lang.Object);
    descriptor: (Ljava/lang/Object;Ljava/lang/Object;)V
    flags: ACC_STATIC
    Code:
      stack=2, locals=3, args_size=2
         0: aload_0
         1: invokestatic  #16                 // Method java/lang/invoke/DirectMethodHandle.internalMemberName:(Ljava/lang/Object;)Ljava/lang/Object;
         4: astore_2
         5: aload_1
         6: aload_2
         7: checkcast     #18                 // class java/lang/invoke/MemberName
        10: invokestatic  #24                 // Method java/lang/invoke/MethodHandle.linkToStatic:(Ljava/lang/Object;Ljava/lang/invoke/MemberName;)V
        13: return
    RuntimeVisibleAnnotations:
      0: #8()
      1: #9()
      2: #10()

  static void dummy();
    descriptor: ()V
    flags: ACC_STATIC
    Code:
      stack=1, locals=0, args_size=0
         0: ldc           #28                 // String DMH.invokeStatic_001_L_V=Lambda(a0:L,a1:L)=>{\n    t2:L=DirectMethodHandle.internalMemberName(a0:L);\n    t3:V=MethodHandle.linkToStatic(a1:L,t2:L);void}
         2: pop
         3: return
}
SourceFile: "LambdaForm$DMH001"
```
这个适配器将获取方法句柄中的 MemberName 类型的字段，并且以它为参数调用 linkToStatic 方法。Java 虚拟机也会对 linkToStatic 调用做特殊处理，它将根据传入的 MemberName 参数所存储的方法地址或者方法
表索引，直接跳转至目标方法。

前面提到，Invokers.checkCustomized 方法会在方法句柄的执行次数超过一个阈值时进行优化。实际上，方法句柄一开始持有的适配器是共享的，当它被多次调用之后，Invokers.checkCustomized 方法会为该方法句柄生成一个特有的适配器。
这个特有的适配器会将方法句柄作为常量，直接获取其 MemberName 类型的字段，并继续后面的 linkToStatic 调用。

因此，方法句柄的调用和反射调用一样，都是间接调用，也会面临无法内联的问题。不过，与反射调用不同的是，方法句柄的内联瓶颈在于即时编译器能否将该方法句柄识别为常量。


#### 4. invokedynamic指令
invokedynamic是Java 7引入的一条新指令，用以支持动态语言的方法调用。它将调用点（CallSite）抽象成一个Java类，并且将原本由Java虚拟机控制的方法调用以及方法链接暴露给应用程序。在运行过程中，每一条invokedynamic指令将捆绑一个调用点，并且会调用
该调用点所链接的方法句柄。（invokedynamic - CallSite - MethodHandle）

在第一次执行invokedynamic指令时，Java虚拟机会调用该指令所对应的启动方法（BootStrap Method）生成调用点（CallSite），并且将之绑定到该invokedynamic指令中。在之后的运行过程中，Java虚拟机则会直接调用绑定的调用点所链接的方法句柄。

在字节码中，启动方法是用方法句柄来指定的。这个方法句柄指向一个返回类型为调用点的静态方法。该方法必须接收三个固定的参数，分别为一个 Lookup 类实例，一个用来指代目标方法名字的字符串，以及该调用点能够链接的方法句柄的类型。除了这三个必需
参数之外，启动方法还可以接收若干个其他的参数，用来辅助生成调用点，或者定位所要链接的目标方法。
```
public static CallSite bootstrap(MethodHandles.Lookup l, String name, MethodType callSiteType) throws Throwable {
    MethodHandle mh = l.findVirtual(Horse.class, name, MethodType.methodType(void.class));
    return new ConstantCallSite(mh.asType(callSiteType));
}
```