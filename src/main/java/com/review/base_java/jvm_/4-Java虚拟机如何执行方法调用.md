[《深入拆解 Java 虚拟机》04 | JVM是如何执行方法调用的？（上）](https://time.geekbang.org/column/article/11539)

[《深入拆解 Java 虚拟机》05 | JVM是如何执行方法调用的？（下）](https://time.geekbang.org/column/article/12098)

---

#### 1. Java程序中的重载与重写
- 重载：指方法名相同而参数类型不相同的方法之间的关系。重载除了同一个类中的方法之外，也可以作用于该类所继承而来的方法（如果子类定义了与父类中非私有方法同名的方法，且这两个方法的参数类型不同，那么
在子类中，这两个方法同样构成了重载）

    - 重载的方法在编译过程中即可完成识别。具体到每一个方法调用，Java编译器会根据所传入的参数的声明类型来选取重载方法。选取的过程分为三个阶段：
        1. 在不考虑对基本类型自动装拆箱（auto-boxing，auto-unboxing），以及可变长参数的情况下选取重载方法。
        2. 如果在第 1 个阶段中没有找到适配的方法，那么在允许自动装拆箱，但不允许可变长参数的情况下选取重载方法。
        3. 如果在第 2 个阶段中没有找到适配的方法，那么在允许自动装拆箱以及可变长参数的情况下选取重载方法。
    - 如果Java编译器在同一个阶段找到了多个适配的方法，那么它会在其中选择一个最为贴切的，而决定贴切程度的一个关键就是形式参数类型的继承关系。比如：两个匹配的重载方法，第一个参数为Object，第二个
    参数为String，由于String是Object的子类，Java编译器会认为第二个方法更为贴切。

- 重写：指方法名相同并且参数类型也相同的方法之间的关系。

#### 2.JVM的静态绑定和动态绑定
Java 虚拟机与 Java 语言不同，它识别方法（包括重载、重写）的关键在于类名、方法名以及方法描述符（method descriptor），其中方法描述符是由方法的参数类型以及返回类型所构成。
- 重载：指方法名相同而方法描述符不相同。由于对重载方法的区分在编译阶段已经完成，我们也可以认为 Java 虚拟机不存在重载这一概念。
- 重写：指方法名相同且方法描述符也相同。对于Java 语言中重写而 Java 虚拟机中非重写的情况，编译器会通过生成“桥接方法”来实现 Java 中的重写语义。详见：[Java中bridge方法探秘](http://chaser520.iteye.com/admin/blogs/2432696)

由于某个类中的重载方法可能被它的子类所重写，因此 Java 编译器会将所有对非私有实例方法的调用编译为需要动态绑定的类型。确切地说，Java 虚拟机中的静态绑定指的是在解析时便能够直接识别目标方法的情况，
而动态绑定则指的是需要在运行过程中根据调用者的动态类型来识别目标方法的情况。具体来说，Java 字节码中与调用相关的指令共有五种：
- invokestatic：用于调用静态方法。
- invokespecial：用于调用私有实例方法、构造器，以及使用 super 关键字调用父类的实例方法或构造器，和所实现接口的默认方法。
- invokevirtual：用于调用非私有实例方法。
- invokeinterface：用于调用接口方法。
- invokedynamic：用于调用动态方法。

对于 invokestatic 以及 invokespecial 而言，Java 虚拟机能够直接识别具体的目标方法（非虚方法，静态绑定）。而对于 invokevirtual 以及 invokeinterface 而言，在绝大部分情况下，虚拟机需要在执行过程中，根据调用者的动态类型，
来确定具体的目标方法（虚方法，动态绑定）。唯一的例外在于，如果虚拟机能够确定目标方法有且仅有一个，比如说目标方法被标记为 final，那么它可以不通过动态类型，直接确定目标方法（虚方法，静态绑定）。

#### 3. 调用指令的符号引用
在编译过程中，我们并不知道目标方法的具体内存地址，Java 编译器会暂时用符号引用来表示该目标方法。这一符号引用包括目标方法所在的类或接口的名字，以及目标方法的方法名和方法描述符。在执行使用了符号
引用的字节码前，Java 虚拟机需要解析这些符号引用，并替换为实际引用。

符号引用存储在 class 文件的常量池之中。根据目标方法是否为接口方法，这些引用可分为接口符号引用和非接口符号引用。
- 对于接口符号引用，假定该符号引用所指向的接口为 I，则 Java 虚拟机会按照如下步骤进行查找：
    1. 在I中查找符合名字及描述符的方法。
    2. 如果没有找到，在Object类中的公有实例方法中搜索。
    3. 如果没有找到，则在I的超接口中搜索。这一步搜索得到的目标方法必须是非私有、非静态的。并且，如果目标方法在超接口中，则需满足 I 与该接口之间没有其他符合条件的目标方法。如果有多个符合条件的
    目标方法，则任意返回其中一个。
- 对于非接口符号引用，假定该符号引用所指向的类为 C，则 Java 虚拟机会按照如下步骤进行查找：
    1. 在 C 中查找符合名字及描述符的方法。
    2. 如果没有找到，在 C 的父类中继续搜索，直至 Object 类。
    3. 如果没有找到，在 C 所直接实现或间接实现的接口中搜索，这一步搜索得到的目标方法必须是非私有、非静态的。并且，如果目标方法在间接实现的接口中，则需满足 C 与该接口之间没有其他符合条件的目标方法。
    如果有多个符合条件的目标方法，则任意返回其中一个。

经过上述的解析步骤之后，符号引用会被解析成实际引用。对于可以静态绑定的方法调用而言，实际引用是一个指向方法的指针。对于需要动态绑定的方法调用而言，实际引用则是一个方法表的索引。

#### 4. 虚方法调用
在 Java 虚拟机中，静态绑定包括用于调用静态方法的 invokestatic 指令，和用于调用构造器、私有实例方法以及超类非私有实例方法的 invokespecial 指令。如果虚方法调用指向一个标记为 final 的方法，那么 Java 虚拟机也可以静态绑定该虚方法调用的目标方法。除此之外，
Java 里所有非私有实例方法调用都会被编译成 invokevirtual 指令，而接口方法调用都会被编译成 invokeinterface 指令，这两种指令均属于 Java 虚拟机中的虚方法调用。在绝大多数情况下，Java虚拟机需要根据调用者的动态类型来确定虚方法调用的目标方法（动态绑定），采
用一种用空间换取时间的策略实现，即为每个类生成一张方法表，用以快速定位目标方法。


##### 4.1 方法表
方法表是在链接-准备阶段构建的，invokevirtual 所使用的虚方法表和invokeinterface 所使用的接口方法表略有不同，但是原理是类似的，后面以invokevirtual 所使用的虚方法表进行分析。

方法表本质上是一个数组，每个数组元素指向一个当前类及其祖先类中非私有的实例方法。这些方法可能是具体的、可执行的方法，也可能是没有相应字节码的抽象方法。方法表满足两个特质：其一，子类方法表中包含父类方法表中的所有方法；其二，子类方法在方法表中的索引值，
与它所重写的父类方法的索引值相同。在解析过程中，Java虚拟机将获取调用者的实际类型，并在该实际类型的虚方法表中，根据索引值获得目标方法，这个过程就是动态绑定。实际上，使用了方法表的动态绑定与静态绑定相比，仅仅多出几个内存解引用操作：访问栈上的调用者，
读取调用者的动态类型，读取该类型的方法表，读取方法表中某个索引值所对应的目标方法。相对于创建并初始化 Java 栈帧来说，这几个内存解引用操作的开销简直可以忽略不计。

##### 4.2 内联缓存
内联缓存是一种加快动态绑定的优化技术。它能够缓存虚方法调用中调用者的动态类型，以及该类型所对应的目标方法。在之后的执行过程中，如果碰到已缓存的类型，内联缓存便会直接调用该类型所对应的目标方法。如果没有碰到已缓存的类型，内联缓存则会退化至使用基于方法表的
动态绑定。

Java 虚拟机所采用的单态内联缓存将纪录调用者的动态类型，以及它所对应的目标方法。当碰到新的调用者时，如果其动态类型与缓存中的类型匹配，则直接调用缓存的目标方法。否则，Java 虚拟机将该内联缓存劣化为超多态内联缓存，在今后的执行过程中直接使用方法表进行动态绑定。

```
针对多态优化，通常会提及以下三个术语：
    1. 单态：指的是仅有一种状态的情况
    2. 多态：指的是有限数量种状态的情况。二态是多态的其中一种。
    3. 超多态：指的是更多种状态的情况。通常用一个具体数值来区分多态和超多态。

对于内联缓存来说，有对应的单态内联缓存、多态内联缓存、超多态内联缓存。
    1. 单态内联缓存：只缓存一种动态类型以及它所对应的目标方法。
    2. 多态内联缓存：缓存了多个动态类型及其目标方法。它需要逐个将所缓存的动态类型与当前动态类型进行比较，如果命中，则调用对应的目标方法。
    3. 超多态内联缓存：与多态内联缓存相同。

在实践中，大部分的虚方法调用均是单态的，为了节省内存空间，Java虚拟机只采用单态内联缓存。若没有命中内联缓存，Java虚拟机将内联缓存劣化为超多态内联缓存（即放弃使用内联缓存进行优化），直接使用方法表进行动态绑定。
```

#### 4.3 方法内联
- 任何方法调用除非被内联，否则都会有固定开销。这些开销来源于保存程序在该方法中的执行位置、以及新建、压入和弹出新方法所使用的栈帧。